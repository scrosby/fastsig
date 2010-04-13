package org.rice.crosby.historytree.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.rice.crosby.historytree.*;
import org.rice.crosby.historytree.aggs.*;
import org.rice.crosby.historytree.generated.Serialization;
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
		System.out.println(pb.toString());
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
	}

	HistoryTree<String, String> makeHistTree() {
		List<String> x = Arrays.asList("Alan","Bob","Charlie","Dan","Elen","Frank","Gordon","Helen","Isis","Jon","Kevin");
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		ArrayStore<String,String> datastore = new ArrayStore<String,String>();
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
		//System.out.println(String.format("Doing(%d/%d)\n",version,histtree.version()));
		HashStore<String,String> datastore=new HashStore<String,String>();
		HistoryTree<String,String> clone= histtree.makePruned(datastore);
		assert histtree.version() == clone.version();
		try {
			clone.copyV(histtree, version, copyValFlag);
			clone.aggV(version);
			assertEquals(histtree.aggV(version),clone.aggV(version));
			System.out.println(histtree.toString(String.format(" Orig( ):")));			
			System.out.println(clone.toString(String.format("Clone(%d):",version)));			
			assertEquals(histtree.agg(),clone.agg());
		} catch (ProofError e) {
			System.out.println("Unable to copy");
			e.printStackTrace();
		}	
		//System.out.println(clone.toString(String.format("Clone(%d):",version)));
	}
	private void helpTestMakePrunedPair(HistoryTree<String,String> histtree, int version1, int version2, boolean copyValFlag) {
		//System.out.println(String.format("Doing(%d+%d/%d)\n",version1,version2,histtree.version()));
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
		//for (int i = 0 ; i <= histtree.version() ; i++) {
		//	helpTestMakePruned(histtree,i,false);
		//}
	}		

	@Test
	public void testMakePrunedPair() {
		HistoryTree<String,String> histtree= makeHistTree();
		for (int i = 0 ; i <= histtree.version() ; i++) {
			for (int j = 0 ; j <= histtree.version() ; j++) {
				helpTestMakePrunedPair(histtree,i,j,true);
			}
		}
	}
}



