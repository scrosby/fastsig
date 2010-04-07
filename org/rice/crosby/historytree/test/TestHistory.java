package org.rice.crosby.historytree.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.rice.crosby.historytree.*;
import org.rice.crosby.historytree.aggs.*;

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
		for (i=0 ; i<histtree.version(); i++) {
			System.out.format("AggV(%d) %s\n",i,histtree.aggV(i));
		}
	}

	void helpTestMakePruned(HistoryTree<String,String> histtree, int version, boolean copyValFlag) {
		HashStore<String,String> datastore=new HashStore<String,String>();
		HistoryTree<String,String> clone= new HistoryTree<String,String>(histtree.getAggObj(),datastore);
		try {
			clone.copyV(histtree, version, copyValFlag);
		} catch (ProofError e) {
			System.out.println("Unable to copy");
			e.printStackTrace();
		}	
		System.out.println(clone.toString());
	}
	void testMakePruned(HistoryTree<String,String> histtree) {
		for (int i = 0 ; i <= histtree.version() ; i++) {
			helpTestMakePruned(histtree,i,false);
		}
		for (int i = 0 ; i <= histtree.version() ; i++) {
			helpTestMakePruned(histtree,i,false);
		}
	}		
}



