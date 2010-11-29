package edu.rice.historytree;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.rice.historytree.*;
import edu.rice.historytree.aggs.*;
import edu.rice.historytree.generated.Serialization;
import edu.rice.historytree.storage.ArrayStore;
import edu.rice.historytree.storage.HashStore;

import junit.framework.TestCase;

public class TestMerkle extends TestCase {
	public final String NAMES[]  = {
			"Alan","Bob","Charlie","Dan",
			"Elen","Frank","Gordon","Helen",
			"Isis","Jon","Kevin", "Laura"}; 

	public String results[] = {
			"A",
			"[A,B]",
			"[[A,B],[C,<>]]", "[[A,B],[C,D]]",
			"[[[A,B],[C,D]],[[E,<>],<>]]",
			"[[[A,B],[C,D]],[[E,F],<>]]",
			"[[[A,B],[C,D]],[[E,F],[G,<>]]]",
			"[[[A,B],[C,D]],[[E,F],[G,H]]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,<>],<>],<>]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,J],<>],<>]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,J],[K,<>]],<>]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,J],[K,L]],<>]]",
	};
	
	public void doTestAppendOnStore(int len, HistoryDataStoreInterface<String,String> store) {
		String mynames[] = Arrays.copyOf(NAMES,len);
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		MerkleTree<String,String> histtree=new MerkleTree<String,String>(aggobj,store);

		
		for (int i = 0 ; i < len ; i++) {
			histtree.append(mynames[i]) ; 
		}
		System.out.println(histtree.toString(String.format("Freeze1(%d)",len)));
		histtree.freeze();
		System.out.println(histtree.toString(String.format("Freeze2(%d)",len)));
		assertEquals(results[len-1],histtree.agg());
	}

	@Test
	public void testOnArrayStore() {
		for (int len = 1 ; len < 12 ; len++) {
			HistoryDataStoreInterface<String,String> store = new ArrayStore<String,String>();
			doTestAppendOnStore(len,store);
		}
	}

	@Test
	public void testOnHashStore() {
		for (int len = 1 ; len < 12 ; len++) {
		HistoryDataStoreInterface<String,String> store = new HashStore<String,String>();
		doTestAppendOnStore(len,store);
	}
	}
	
	MerkleTree<String, String> makeHistTree(int length) {
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		HistoryDataStoreInterface<String,String> datastore = new ArrayStore<String,String>();
		MerkleTree<String,String> histtree=new MerkleTree<String,String>(aggobj,datastore);
		
		for (int i=0 ; i < length ; i++) {
			histtree.append(NAMES[i]);
		}
		histtree.freeze();
		return histtree;
	}
	

	void doTestMakePruned(int length, HistoryDataStoreInterface<String,String> datastore) throws ProofError {
		MerkleTree<String,String> tree=makeHistTree(length);

		MerkleTree<String,String> clone0=tree.makePruned(datastore);		

		assertEquals(clone0.version(),length-1);
		assertEquals(tree.agg(),clone0.agg());

		
		for (int i=0; i <= length-1 ; i++) {
			MerkleTree<String,String> clone=tree.makePruned(datastore);		
			clone.copyV(tree,i,true);
			assertEquals(length-1,clone.version());
			assertEquals(NAMES[i],clone.leaf(i).getVal());
			assertEquals(results[length-1],clone.agg());
		}
	}
	
	@Test
	public void testPruned() throws ProofError {
		// Try around powers of 2.
		for (int i=1 ; i < 12 ; i++) {
			HashStore<String,String> store = new HashStore<String,String>();
			doTestMakePruned(i,store);
		}
	}

	
	// TO WRITE TESTS BELOW HERE.
	
	@Test	
	public void testSerialization() throws InvalidProtocolBufferException {
		MerkleTree<String,String> histtree= makeHistTree(11);
		byte[] serialized = histtree.serializeTree();
		MerkleTree<String,String> tree2 = parseSerialization(serialized);
		System.out.println(tree2.toString("Unserial:"));
		assertEquals(histtree.agg(),tree2.agg());
	}
	
	
	public MerkleTree<String,String> parseSerialization(byte serialized[]) throws InvalidProtocolBufferException {
		Serialization.PrunedTree.Builder builder = Serialization.PrunedTree.newBuilder();
		Serialization.PrunedTree pb = builder.mergeFrom(serialized).build();
		//System.out.println(pb.toString());
		MerkleTree<String,String> tree2= new MerkleTree<String,String>(new ConcatAgg(),new HashStore<String,String>());
		tree2.updateTime(pb.getVersion());
		tree2.parseTree(pb);
		return tree2;
	}	
	MerkleTree<byte[],byte[]> 
	makeShaHistTree() {
		List<String> x = Arrays.asList("Alan","Bob","Charlie","Dan","Elen","Frank","Gordon","Helen","Isis","Jon","Kevin");
		AggregationInterface<byte[],byte[]> aggobj = new SHA256AggB64();
		ArrayStore<byte[],byte[]> datastore = new ArrayStore<byte[],byte[]>();
		MerkleTree<byte[],byte[]> histtree=new MerkleTree<byte[],byte[]>(aggobj,datastore);
		
		for (String s : x) {
			histtree.append(s.getBytes());
		}
		histtree.freeze();
		System.out.println(aggobj.serializeAgg(histtree.agg()).toStringUtf8());
		System.out.println(histtree.toString("Binary:"));
		return histtree;
	}
	@Test
	public void testMakeShaHistTree() {
		makeShaHistTree();
	}
	public MerkleTree<byte[],byte[]> parseSerialization2(byte serialized[]) throws InvalidProtocolBufferException {
		Serialization.PrunedTree.Builder builder = Serialization.PrunedTree.newBuilder();
		Serialization.PrunedTree pb = builder.mergeFrom(serialized).build();
		//System.out.println(pb.toString());
		MerkleTree<byte[],byte[]> tree2= new MerkleTree<byte[],byte[]>(new SHA256AggB64(),new HashStore<byte[],byte[]>());
		tree2.updateTime(pb.getVersion());
		tree2.parseTree(pb);
		return tree2;
	}	


	public void benchTestCore(int keycount, int iter, boolean doGetAgg, boolean doGetAggV, boolean doMakePrune,
			boolean doAddPruned, boolean doSerialize, boolean doDeserialize, boolean doVf) {
		int LOOP = keycount; // TODO: BUGGY WITH THIS AN EXACT POWER OF 2.
		MerkleTree<byte[],byte[]> histtree;
		for (int i=0; i < iter ; i++) {
			AggregationInterface<byte[],byte[]> aggobj = new SHA256AggB64();
			ArrayStore<byte[],byte[]> datastore = new ArrayStore<byte[],byte[]>();
			histtree=new MerkleTree<byte[],byte[]>(aggobj,datastore);
			for (int j =0; j < LOOP ; j++) {
				histtree.append(String.format("Foo%d",j).getBytes());

			}
			histtree.freeze();
			if (doGetAgg)
				histtree.agg();
			if (doGetAggV)
				;
			if (doMakePrune||doAddPruned||doSerialize||doDeserialize||doVf) {
				for (int j = 0 ; j < LOOP ; j++) {
					HashStore<byte[],byte[]> datastore2 = new HashStore<byte[],byte[]>();
					MerkleTree<byte[],byte[]> clone = histtree.makePruned(datastore2);
					if (doAddPruned) {
						try {
							clone.copyV(histtree, j, true);
						} catch (ProofError e) {
							e.printStackTrace();
						}
					}

					//System.out.print(clone.toString("Clone:"));
					if (doDeserialize) {
						byte[] data = clone.serializeTree();
						if (doSerialize) {
							MerkleTree<byte[],byte[]> parsed=null;
							try {
								parsed = parseSerialization2(data);
							} catch (InvalidProtocolBufferException e) {
								e.printStackTrace();
							}
							//System.out.print(parsed.toString("Parsed:"));
							if (doVf) {
								assertTrue(Arrays.equals(parsed.agg(),histtree.agg()));
								assertTrue(Arrays.equals(parsed.leaf(j).getVal(),String.format("Foo%d",j).getBytes()));
								assertTrue(Arrays.equals(parsed.leaf(j).getAgg(),histtree.leaf(j).getAgg()));
								assertTrue(Arrays.equals(parsed.leaf(j).getVal(),histtree.leaf(j).getVal()));
							}
						}
					}
				}
			}

		}
	}


	final int LOOPCOUNT = 1;

	@Test 
	public void testdoAppend() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,false,false,false,false,false,false,false);
			benchTestCore(13,10,false,false,false,false,false,false,false);
		}
	}
	@Test 
	public void testdoGetAgg() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,true,false,false,false,false,false,false);
			benchTestCore(13,10,true,false,false,false,false,false,false);
		}
	}
	@Test 
	public void testdoGetAggV() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,false,true,false,false,false,false,false);
			benchTestCore(13,10,false,true,false,false,false,false,false);
		}
	}
	@Test 
	public void testdoSimplePruned() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,false,false,true,false,false,false,false);
			benchTestCore(13,10,false,false,true,false,false,false,false);
		}
	}
	@Test 
	public void testdoAddPruned() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,false,false,false, true,false,false,false);
			benchTestCore(13,10,false,false,false, true,false,false,false);
		}
	}
	@Test 
	public void testdoAddPrunedSerialize() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,false,false,false, true,true,false,false);
			benchTestCore(13,10,false,false,false, true,true,false,false);
		}
	}
	@Test 
	public void testdoAddPrunedDeSerialize() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,false,false,false,true,true,true,false);
			benchTestCore(13,10,false,false,false,true,true,true,false);
		}
	}

	@Test 
	public void testdoAddPrunedDeSerializeVf() {
		for (int i = 0 ; i < LOOPCOUNT ; i++) {
			benchTestCore(8,10,false,false,false,true,true,true,true);
			benchTestCore(13,10,false,false,false,true,true,true,true);
		}
	}
}


