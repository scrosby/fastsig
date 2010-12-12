package edu.rice.batchsig.lazy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.google.protobuf.ByteString;

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.Verifier;
import edu.rice.batchsig.HistTreeTools;
import edu.rice.historytree.HistoryTree;

/** Represent all of the message from one history tree instance by an author. Track dependencies, users, etc. */
public class OneTree {
	/** How many messages are in the history tree. */
	int size = 0;
	
	/** The public key signature primitives. */
	final private SignaturePrimitives signer;
	
	/** Map from an integer version number to the message at that version number. */
	HashMap<Integer, IMessage> bundles = new LinkedHashMap<Integer, IMessage>(1,.75f, false);
	
	/** Cache of root hashes for each unvalidated bundle.
	 *    When we verify a splice, we need to take the predecessor's bundle agg() and compare it to aggV(pred.version).
	 *    Rather than rebuild the pred's pruned tree, or cache the whole thing, just cache the parts we need, the agg().
	 *  */
	HashMap<Integer, ByteString> roothashes = new HashMap<Integer, ByteString>();

	/**
	 * This hashmap finds the message signatures we need to verify to validate a
	 * given bundle.
	 * 
	 * When we find the 'root' node of a dependency tree, that will be an
	 * exlempar, but that root isn't really a message. It is the history tree version of a different message.
	 * This map tells us which message has that root, so that we can use its public signature to verify the tree.
	 * 
	 */
	HashMap<Integer, IMessage> validators = new HashMap<Integer, IMessage>();
	
	/**
	 * Invariant; The dag contains nodes for each message and the version
	 * numbers of the splicepoints seen for that bundle.
	 * 
	 * If an edge is in the dag and both bundles have been seen (are in the
	 * bundles hash), then the splice has been VERIFIED.
	 * 
	 * If an edge is in the dag, but either the from or to bundles in the
	 * dag are *NOT* in the bundles hash, then the splice is provisional
	 * and may not exist. We wait until the peer message arrives, verify the
	 * splice, and keep the edge if the splice verifies, or remove the edge
	 * if the splice fails.
	 * 
	 * */
	final private Dag<Integer> dag = new Dag<Integer>();

	/** Which author created the history tree that we are tracking? */
	final private Object author;
	/** Which treeid did the author assign to the tree that we are tracking? */
	final private long treeid;

	/** Make a OneTree around a given author, treeid, and public key verification object. */
	public OneTree(SignaturePrimitives signer, Object author, long treeid) {
		this.author = author;
		this.treeid = treeid;
		this.signer = signer;
	}

	
	/** How many messages are in this OneTree */
	public int size() {
		return size;
	}

	/** @return The author of the history tree we are tracking. */
	public Object getAuthor() {
		return author;
	}

	/** @return The treeid assigned by the author of the history tree we are tracking. */
	public long getTreeid() {
		return treeid;
	}

	/** Make or get the node in the Dag corresponding to the given message. */
	Dag<Integer>.DagNode getNode(IMessage m) {
		Integer key = m.getSignatureBlob().getLeaf();
		return dag.makeOrGet(key);
	}

		
	/** Each node in the dag corresponds to a set of bundles. All that end in the same epoch. */
	void addMessage(IMessage m) {
		//System.out.println("\nAdding message "+m);
		size++;
		Integer key = m.getSignatureBlob().getLeaf();
		Integer bundlekey = m.getSignatureBlob().getTree().getVersion();
		HistoryTree<byte[],byte[]> tree = HistTreeTools.parseHistoryTree(m);
		ByteString agg = ByteString.copyFrom(tree.agg());
		
		// First, see if this message is well-formed in the bundle?   Yes. It is.
		if (!Verifier.checkLeaf(m,tree)) {
			System.out.println("Broken proof that doesn't validate message in proof.");
			m.signatureValidity(false);
			size--;
			return;
		}
		
		// First, have we seen this ending bundle before?
		if (validators.containsKey(bundlekey)) {
			// We got a bundle already. Does it have the same agg?
			if (!roothashes.get(bundlekey).equals(agg)) {
				// PROBLEM: Have an inconsistent bundle already.
				// TODO: Can't handle these at all. Only solution: Verify it immediately.
				throw new Error("TODO");
			}
			//System.out.println("Adding to existing bundle");
		}
		validators.put(bundlekey, m);
		
		// At this point, we know that any keys in this bundle all have the same
		// ending hash, ergo, the same contents. We don't have to worry about inconsistency
		// anymore, and can just store the data, except for validating splices.

		// Now, build an edge in the dag from the integer representing the bundle to this message.
		Dag<Integer>.DagNode node = dag.makeOrGet(key);
		Dag<Integer>.DagNode bundlenode = dag.makeOrGet(bundlekey);

		// Add a dependency edge for the bundle.
		if (!node.get().equals(bundlenode.get()))
			dag.addEdge(bundlenode, node);
		// And put the message in.
		bundles.put(key,m);
				
		// PART 1: See if we've seen later bundles we might splice into.
		// This case should be rare and only occur when bundles arrive out-of-order.
		for (Dag<Integer>.DagNode succ : bundlenode.getParents()) {
			// For each later message in the dag that provisionally splices this message.
			//System.out.println("Looking at later bundles");
			Integer succi = succ.get();
			IMessage succm = bundles.get(succi);
			if (succm == null)
				throw new Error("Algorithm bug.");
			// Time to verify the splice is OK. 
			HistoryTree<byte[],byte[]> succtree = HistTreeTools.parseHistoryTree(succm);
			if (Arrays.equals(succtree.aggV(bundlekey.intValue()),tree.agg())) {
				System.out.println("Unusual splice circumstance -- success");
				dag.addEdge(succ,bundlenode);
			} else {
				// Splice fails. Remove the edge.
				System.out.println("Unusual splice circumstance -- failure & removal");
			}
		}
			
		// PART 2: See which prior bundles we splice into.
		for (Integer predi : m.getSignatureBlob().getSpliceHintList()) {
			ByteString aggv = ByteString.copyFrom(tree.aggV(predi.intValue()));
			//System.out.format("Handling splicehint %d with hash %d\n",predi,aggv.hashCode());
			// For each splicepoint to prior bundles in this message,
			IMessage predm = validators.get(predi);
			Dag<Integer>.DagNode prednode = dag.makeOrGet(predi);
			// Have we seen the prior message?
			if (predm == null) {
				//System.out.println("No priors found, but adding edge anyways.");
				// Nope. Add the node to the dag. Add an edge to that child; it'll be provisional
				dag.addEdge(node,prednode);
			} else {
				//System.out.format("Agg(%d)=%d of pred\n",predm.getSignatureBlob().getTree().getVersion(),roothashes.get(predi).hashCode());
				// We have seen that message. We need to check the splice.
				if (aggv.equals(roothashes.get(predi))) {
					//System.out.println("Found a prior. Verified the splice!");
					dag.addEdge(node,prednode);
				} else {
					// Splice fails. Remove the edge.
					//System.out.println("Found a prior, but splice failed");
					prednode = dag.makeOrGet(predi);
				}
			}
		}
		//System.out.format("Stored roothash at (%d) of %d of pred\n",bundlekey,agg.hashCode());
		roothashes.put(bundlekey,agg);
		bundles.put(key, m);
		//System.out.println("Finished handling for message");
	}

	/** Called to remove a real message from all tracking */
	private void remove(IMessage m) {
		int index = m.getSignatureBlob().getLeaf();
		remove(index);
	}
	
	/** Called to remove a message index from all tracking */
	private void remove(int index) {
		if (bundles.remove(index)!= null)
			size--;
		validators.remove(index);
		roothashes.remove(index);
		}

	/** Force the oldest thing here.
	 * 
	 *  @return true if something was forced. */
	boolean forceOldest() {
		System.out.format("Forcing oldest message (OneTree)\n");

		Iterator<Integer> i = bundles.keySet().iterator();
		//System.out.format("%d == %d?\n",size,bundles.size());
		if (!i.hasNext()) {
			if (size != 0)
				throw new Error("Size should be zero");
			return false;
		}
		IMessage m = bundles.get(i.next());
		m.resetCreationTimeNull();
		forceMessage(m);
		return true;
	}
	
	
	/** Force this specific message. */
	void forceMessage(IMessage m) {
		//System.out.format("\n>>>>> Forcing a message %d to %s\n",m.getSignatureBlob().getLeaf(),getName());
		
		if (!bundles.containsKey(m.getSignatureBlob().getLeaf())) {
			System.out.println("Forcing message that doesn't exist:" + m.toString()); // Should trigger occasionally.
			throw new Error("WARN");
			//return;
		}
		
		Dag<Integer>.DagNode node = getNode(m);

		// Step one: Find a root.
		Dag<Integer>.Path rootPath = dag.rootPath(node);
		// Step two, until we find a root whose signature verifies.
		while (true) {
			//System.out.println("WhileLoop at rootPath ="+rootPath);
			Dag<Integer>.DagNode root = rootPath.root();
			Integer rooti = root.get();
			// An incoming message that nominally validates the root bundle (may be more than one)
			IMessage rootm = validators.get(rooti);
			//System.out.format("Got root at %d about to see if it verifies %s\n",rooti,rootm);
			HistoryTree<byte[], byte[]> roottree = HistTreeTools.parseHistoryTree(rootm);

			// Verify the root's public key signature.
			if (HistTreeTools.verifyHistoryRoot(signer, rootm, roottree)) {
				//System.out.println("Verified the root's signature - SUCCESS. It is valid");
				// Success!
				// Now traverse *all* descendents and mark them as good.
				Collection<Dag<Integer>.DagNode> descendents = dag.getAllChildren(root);
				for (Dag<Integer>.DagNode i : descendents) {
					//System.out.println("Traversing descendent to mark as valid:"+i.get());
					//Integer desci = i.get();
					IMessage descm = bundles.get(i.get());
					if (descm != null) {
						// TODO: Cache the spliced predecessor hashes from this node as being valid?
						//System.out.println("... and marking it as good!");
						// This message is not provisional. It is valid.
						descm.signatureValidity(true);
						// Remove the message from further tracking.
						remove(descm);
					}
					// Remove any vestiges of the node.
					remove(i.get());
					i.remove();
				}

				//System.out.format("<<<<<< Done with handling force of %d to %s\n",m.getSignatureBlob().getLeaf(),getName());
				return;
			} else {
				System.out.println("Failed the root's signature");
				rootm.signatureValidity(false);
				remove(rootm);
			}
			System.out.println("Got a problem, need to try the next root");

			// Failure. Try the next root.
			rootPath.next();
		}
		
		
	}
	
	/** Force every message in the OneTree */
	public void forceAll() {
		//System.out.format("Forcing all bundles in OneTree\n");
		while (!bundles.isEmpty()) {
			IMessage m = bundles.entrySet().iterator().next().getValue();
			m.resetCreationTimeNull();
			forceMessage(m);
		}
	}
}