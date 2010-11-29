package edu.rice.historytree;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.rice.historytree.*;
import edu.rice.historytree.aggs.*;
import edu.rice.historytree.generated.Serialization;
import edu.rice.historytree.storage.AppendOnlyArrayStore;
import edu.rice.historytree.storage.ArrayStore;
import edu.rice.historytree.storage.HashStore;

import junit.framework.TestCase;

public class TestHistory extends TestCase {
	public final String NAMES[]  = {
			"Alan","Bob","Charlie","Dan",
			"Elen","Frank","Gordon","Helen",
			"Isis","Jon","Kevin", "Laura"}; 

	public String results[] = {
			"A",
			"[A,B]",
			"[[A,B],[C,]]", "[[A,B],[C,D]]",
			"[[[A,B],[C,D]],[[E,],]]",
			"[[[A,B],[C,D]],[[E,F],]]",
			"[[[A,B],[C,D]],[[E,F],[G,]]]",
			"[[[A,B],[C,D]],[[E,F],[G,H]]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,],],]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,J],],]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,J],[K,]],]]",
			"[[[[A,B],[C,D]],[[E,F],[G,H]]],[[[I,J],[K,L]],]]",
	};

	
	public HistoryTree<String,String> doTestAppendOnStore(HistoryDataStoreInterface<String,String> store) {
		String mynames[] = Arrays.copyOf(NAMES,9);
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		HistoryTree<String,String> histtree=new HistoryTree<String,String>(aggobj,store);

		
		for (int i = 0 ; i <=4 ; i++) {
			histtree.append(mynames[i]) ; assertEquals(results[i],histtree.agg());
		}
       
        assertEquals(4,histtree.version());
        for (int i = 0 ; i <= histtree.version() ; i++)
        	assertEquals(results[i],histtree.aggV(i));
        
		for (int i = 5 ; i <= 8 ; i++) {
			histtree.append(mynames[i]) ; assertEquals(results[i],histtree.agg());
		}

        assertEquals(8,histtree.version());
        for (int i = 0 ; i <= histtree.version() ; i++)
        	assertEquals(results[i],histtree.aggV(i));

        return histtree;
	}

	@Test
	public void testOnArrayStore() {
		HistoryDataStoreInterface<String,String> store = new ArrayStore<String,String>();
		doTestAppendOnStore(store);
	}
	@Test
	public void testOnAppendArrayStore() {
		HistoryDataStoreInterface<String,String> store = new AppendOnlyArrayStore<String,String>();
		doTestAppendOnStore(store);
	}
	@Test
	public void testOnHashStore() {
		HistoryDataStoreInterface<String,String> store = new HashStore<String,String>();
		doTestAppendOnStore(store);
	}

	
	HistoryTree<String, String> makeHistTree(int length) {
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		HistoryDataStoreInterface<String,String> datastore = new AppendOnlyArrayStore<String,String>();
		HistoryTree<String,String> histtree=new HistoryTree<String,String>(aggobj,datastore);
		
		for (int i=0 ; i < length ; i++) {
			histtree.append(NAMES[i]);
        	assertEquals(results[i],histtree.aggV(i));
		}
		return histtree;
	}
	

	void doTestMakePruned(int length, HistoryDataStoreInterface<String,String> datastore) throws ProofError {
		HistoryTree<String,String> tree=makeHistTree(length);

		HistoryTree<String,String> clone0=tree.makePruned(datastore);		

		assertEquals(clone0.version(),length-1);
		assertEquals(clone0.aggV(tree.version()),clone0.agg());

		
		for (int i=0; i <= length-1 ; i++) {
			HistoryTree<String,String> clone=tree.makePruned(datastore);		
			clone.copyV(tree,i,false);
			assertEquals(length-1,clone.version());
			assertEquals(results[i],clone.aggV(i));
			assertEquals(results[length-1],clone.aggV(length-1));
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
		HistoryTree<String,String> histtree= makeHistTree(11);
		byte[] serialized = histtree.serializeTree();
		HistoryTree<String,String> tree2 = parseSerialization(serialized);
		System.out.println(tree2.toString("Unserial:"));
		tree2.aggV(3);
		assertEquals(histtree.agg(),tree2.agg());
	}
	
	
	public HistoryTree<String,String> parseSerialization(byte serialized[]) throws InvalidProtocolBufferException {
		Serialization.PrunedTree.Builder builder = Serialization.PrunedTree.newBuilder();
		Serialization.PrunedTree pb = builder.mergeFrom(serialized).build();
		//System.out.println(pb.toString());
		HistoryTree<String,String> tree2= new HistoryTree<String,String>(new ConcatAgg(),new HashStore<String,String>());
		tree2.updateTime(pb.getVersion());
		tree2.parseTree(pb);
		return tree2;
	}	
	HistoryTree<byte[],byte[]> 
	makeShaHistTree() {
		List<String> x = Arrays.asList("Alan","Bob","Charlie","Dan","Elen","Frank","Gordon","Helen","Isis","Jon","Kevin");
		AggregationInterface<byte[],byte[]> aggobj = new SHA256AggB64();
		ArrayStore<byte[],byte[]> datastore = new ArrayStore<byte[],byte[]>();
		HistoryTree<byte[],byte[]> histtree=new HistoryTree<byte[],byte[]>(aggobj,datastore);
		
		for (String s : x) {
			histtree.append(s.getBytes());
			System.out.println(aggobj.serializeAgg(histtree.agg()).toStringUtf8());
		}
		System.out.println(histtree.toString("Binary:"));
		return histtree;
	}
	@Test
	public void testMakeShaHistTree() {
		makeShaHistTree();
	}
	public HistoryTree<byte[],byte[]> parseSerialization2(byte serialized[]) throws InvalidProtocolBufferException {
		Serialization.PrunedTree.Builder builder = Serialization.PrunedTree.newBuilder();
		Serialization.PrunedTree pb = builder.mergeFrom(serialized).build();
		//System.out.println(pb.toString());
		HistoryTree<byte[],byte[]> tree2= new HistoryTree<byte[],byte[]>(new SHA256AggB64(),new HashStore<byte[],byte[]>());
		tree2.updateTime(pb.getVersion());
		tree2.parseTree(pb);
		return tree2;
	}	


	public void benchTestCore(int keycount, int iter, boolean doGetAgg, boolean doGetAggV, boolean doMakePrune,
			boolean doAddPruned, boolean doSerialize, boolean doDeserialize, boolean doVf) {
		int LOOP = keycount; // TODO: BUGGY WITH THIS AN EXACT POWER OF 2.
		HistoryTree<byte[],byte[]> histtree;
		for (int i=0; i < iter ; i++) {
			AggregationInterface<byte[],byte[]> aggobj = new SHA256AggB64();
			ArrayStore<byte[],byte[]> datastore = new ArrayStore<byte[],byte[]>();
			histtree=new HistoryTree<byte[],byte[]>(aggobj,datastore);
			for (int j =0; j < LOOP ; j++) {
				histtree.append(String.format("Foo%d",j).getBytes());
				if (doGetAgg)
					histtree.agg();
			}
			if (doGetAggV)
				for (int j = 0 ; j < LOOP ; j++)
					histtree.aggV(j);
			if (doMakePrune||doAddPruned||doSerialize||doDeserialize||doVf) {
				for (int j = 0 ; j < LOOP ; j++) {
					HashStore<byte[],byte[]> datastore2 = new HashStore<byte[],byte[]>();
					HistoryTree<byte[],byte[]> clone = histtree.makePruned(datastore2);
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
							HistoryTree<byte[],byte[]> parsed=null;
							try {
								parsed = parseSerialization2(data);
							} catch (InvalidProtocolBufferException e) {
								e.printStackTrace();
							}
							//System.out.print(parsed.toString("Parsed:"));
							if (doVf) {
								assertTrue(Arrays.equals(parsed.agg(),histtree.agg()));
								assertTrue(Arrays.equals(parsed.aggV(j),histtree.aggV(j)));
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


