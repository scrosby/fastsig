package edu.rice.batchsig;

import org.junit.Test;

import edu.rice.batchsig.lazy.Dag;
import edu.rice.batchsig.lazy.Dag.DagNode;
import junit.framework.TestCase;

public class TestDag extends TestCase {

	
	@Test
	public void testMakeOne() {
		Dag<Integer> dag = new Dag<Integer>();
		Dag<Integer>.DagNode n1 = dag.makeOrGet(1);
		Dag<Integer>.DagNode n2 = dag.makeOrGet(2);
		Dag<Integer>.DagNode n3 = dag.makeOrGet(3);
		Dag<Integer>.DagNode n4 = dag.makeOrGet(4);

		dag.addEdge(n1,n2);
		TestCase.assertTrue(n1.getChildren().contains(n2));
		TestCase.assertTrue(n2.getParents().contains(n1));

		dag.addEdge(n2,n3);
		TestCase.assertTrue(n2.getChildren().contains(n3));
		TestCase.assertTrue(n3.getParents().contains(n2));

		
		
		
	}
	
	
}
