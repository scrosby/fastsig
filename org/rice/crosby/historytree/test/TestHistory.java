package org.rice.crosby.historytree.test;

import org.rice.crosby.historytree.*;
import org.rice.crosby.historytree.aggs.*;

import junit.framework.TestCase;

public class TestHistory extends TestCase {

	void testHistory1() {
		AggregationInterface<String,String> aggobj = new ConcatAgg();
		ArrayStore<String,String> nodefactory = new ArrayStore<String,String>();
		HistoryTreeOps<String,String> histtree=HistoryTreeOps<String,String>();
		
	}
	
	
}
