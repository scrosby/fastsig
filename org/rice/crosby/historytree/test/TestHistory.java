package org.rice.crosby.historytree.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.rice.crosby.historytree.*;
import org.rice.crosby.historytree.aggs.*;
import org.rice.crosby.historytree.generated.Serialization;
import org.rice.crosby.historytree.generated.Serialization.HistTree;
import org.rice.crosby.historytree.storage.AppendOnlyArrayStore;
import org.rice.crosby.historytree.storage.ArrayStore;
import org.rice.crosby.historytree.storage.HashStore;

import com.google.protobuf.InvalidProtocolBufferException;

import junit.framework.TestCase;

public class TestHistory extends TestCase {

	@Test
	public void testAppend() {
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		ArrayStore<String,String> datastore = new ArrayStore<String,String>();
		HistoryTree<String,String> histtree=new HistoryTree<String,String>(aggobj,datastore);
		
		histtree.append("A");
		System.out.println(histtree.agg());
		histtree.append("B");
		System.out.println(histtree.agg());
		histtree.append("C");
		//System.out.println(histtree.toString("AfterC:"));
		System.out.println(histtree.agg());
		histtree.append("D");
        System.out.println(histtree.agg());

        histtree.append("E");
		System.out.println(histtree.agg());
		histtree.append("F");
		System.out.println(histtree.agg());
		histtree.append("G");
		System.out.println(histtree.agg());
        histtree.append("H");
		System.out.println(histtree.agg());

		histtree.append("I");
		System.out.println(histtree.agg());
	}

	public HistoryTree<String,String> parseSerialization(byte serialized[]) throws InvalidProtocolBufferException {
		Serialization.HistTree.Builder builder = Serialization.HistTree.newBuilder();
		Serialization.HistTree pb = builder.mergeFrom(serialized).build();
		//System.out.println(pb.toString());
		HistoryTree<String,String> tree2= new HistoryTree<String,String>(new ConcatAgg(),new HashStore<String,String>());
		tree2.updateTime(pb.getVersion());
		tree2.parseTree(pb);
		return tree2;
	}	
	@Test	
	public void testSerialization() throws InvalidProtocolBufferException {
		HistoryTree<String,String> histtree= makeHistTree();
		byte[] serialized = histtree.serializeTree();
		HistoryTree<String,String> tree2 = parseSerialization(serialized);
		System.out.println(tree2.toString("Unserial:"));
		tree2.aggV(3);
		assertEquals(histtree.agg(),tree2.agg());
	}

	HistoryTree<String, String> makeHistTree() {
		List<String> x = Arrays.asList("Alan","Bob","Charlie","Dan","Elen","Frank","Gordon","Helen","Isis","Jon","Kevin");
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		HistoryDataStoreInterface<String,String> datastore = new AppendOnlyArrayStore<String,String>();
		HistoryTree<String,String> histtree=new HistoryTree<String,String>(aggobj,datastore);
		
		for (String s : x) {
			histtree.append(s);
		}
		return histtree;
	}
	
	
	@Test	
	public void testAggV() {
		HistoryTree<String,String> histtree= makeHistTree();
		System.out.println(histtree.toString("Def:"));

		//assert histtree.version() == 8;
		int i;
		for (i=0 ; i<=histtree.version(); i++) {
			System.out.format("AggV(%d) %s\n",i,histtree.aggV(i));
		}
	}

	private void helpTestMakePruned(HistoryTree<String,String> histtree, int version, boolean copyValFlag) {
		System.out.format("Doing(%d/%d)\n",version,histtree.version());
		//System.out.println(histtree.toString(String.format(" Orig( ):")));			
		HashStore<String,String> datastore=new HashStore<String,String>();
		HistoryTree<String,String> clone= histtree.makePruned(datastore);
		assert histtree.version() == clone.version();
		try {
			//System.out.println(clone.toString(String.format("Prune( ):")));			
			clone.copyV(histtree, version, copyValFlag);
			clone.aggV(version);
			assertEquals(histtree.aggV(version),clone.aggV(version));
			//System.out.println(clone.toString(String.format("Clone(%d):",version)));			
			assertEquals(histtree.agg(),clone.agg());
		} catch (ProofError e) {
			System.out.println("Unable to copy");
			e.printStackTrace();
		}	
		//System.out.println(clone.toString(String.format("Clone(%d):",version)));
	}
	private void helpTestMakePrunedPair(HistoryTree<String,String> histtree, int version1, int version2, boolean copyValFlag) {
		System.out.format("Doing(%d+%d/%d)\n",version1,version2,histtree.version());
		HashStore<String,String> datastore=new HashStore<String,String>();
		HistoryTree<String,String> clone= histtree.makePruned(datastore);
		assert histtree.version() == clone.version();
		try {
			clone.copyV(histtree, version1, copyValFlag);
			clone.copyV(histtree, version2, copyValFlag);
		} catch (ProofError e) {
			System.out.println("Unable to copy");
			e.printStackTrace();
		}	
		//System.out.println(clone.toString(String.format("Clone(%d+%d):",version1,version2)));
	}
	
	@Test
	public void testMakePruned() {
		HistoryTree<String,String> histtree= makeHistTree();
		for (int i = 0 ; i <= histtree.version() ; i++) {
			helpTestMakePruned(histtree,i,true);
		}
		for (int i = 0 ; i <= histtree.version() ; i++) {
			//helpTestMakePruned(histtree,i,false);
		}
	}		

	@Test
	public void testMakePrunedPair() {
		HistoryTree<String,String> histtree= makeHistTree();
		for (int i = 0 ; i <= histtree.version() ; i++) {
			for (int j = 0 ; j <= histtree.version() ; j++) {
				//helpTestMakePrunedPair(histtree,i,j,true);
			}
		}
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
		Serialization.HistTree.Builder builder = Serialization.HistTree.newBuilder();
		Serialization.HistTree pb = builder.mergeFrom(serialized).build();
		//System.out.println(pb.toString());
		HistoryTree<byte[],byte[]> tree2= new HistoryTree<byte[],byte[]>(new SHA256Agg(),new HashStore<byte[],byte[]>());
		tree2.updateTime(pb.getVersion());
		tree2.parseTree(pb);
		return tree2;
	}	


	public void benchTestCore(int iter, boolean doGetAgg, boolean doGetAggV, boolean doMakePrune,
			boolean doAddPruned, boolean doSerialize, boolean doDeserialize, boolean doVf) {
		int LOOP = 1000;
		HistoryTree<byte[],byte[]> histtree;
		for (int i=0; i < iter ; i++) {
			AggregationInterface<byte[],byte[]> aggobj = new SHA256Agg();
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
					if (doDeserialize) {
						byte[] data = clone.serializeTree();
						if (doSerialize) {
							HistoryTree<byte[],byte[]> parsed=null;
							try {
								parsed = parseSerialization2(data);
							} catch (InvalidProtocolBufferException e) {
								e.printStackTrace();
							}

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


	final int LOOPCOUNT = 100;
	
	@Test 
	public void testdoAppend() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
			benchTestCore(10,false,false,false,false,false,false,false);
	}
	@Test 
	public void testdoGetAgg() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
		benchTestCore(10,true,false,false,false,false,false,false);
	}
	@Test 
	public void testdoGetAggV() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
		benchTestCore(10,false,true,false,false,false,false,false);
	}
	
	@Test 
	public void testdoSimplePruned() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
		benchTestCore(10,false,false,true,false,false,false,false);
	}
	@Test 
	public void testdoAddPruned() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
		benchTestCore(10,false,false,false, true,false,false,false);
	}
	@Test 
	public void testdoAddPrunedSerialize() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
		benchTestCore(10,false,false,false, true,true,false,false);
	}
	@Test 
	public void testdoAddPrunedDeSerialize() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
		benchTestCore(10,false,false,false,true,true,true,false);
	}
	
	@Test 
	public void testdoAddPrunedDeSerializeVf() {
		for (int i = 0 ; i < LOOPCOUNT ; i++)
		benchTestCore(10,false,false,false,true,true,true,true);
	}
	
}


