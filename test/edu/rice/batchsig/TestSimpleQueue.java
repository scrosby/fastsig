package edu.rice.batchsig;

import org.junit.Test;

import edu.rice.batchsig.HistoryQueue;
import edu.rice.batchsig.MerkleQueue;
import edu.rice.batchsig.ProcessQueue;
import edu.rice.batchsig.SimpleQueue;
import edu.rice.batchsig.VerifyQueue;

import junit.framework.TestCase;


public class TestSimpleQueue extends TestCase {
	public void insertAndProcess(int offset, ProcessQueue signqueue) {
		MessageWrap msg1 = new MessageWrap(1001);
		MessageWrap msg2 = new MessageWrap(1002);
		MessageWrap msg3 = new MessageWrap(1003);

		signqueue.add(msg1);
		signqueue.add(msg2);
		signqueue.add(msg3);
		signqueue.process();

		// Corrupt message 2.
		msg2.data[1]=0;
		
		msg1.wantValid();
		msg2.wantInValid();
		msg3.wantValid();

		VerifyQueue verifyqueue = new VerifyQueue(new DigestPrimitive());
		verifyqueue.add(msg1);
		verifyqueue.add(msg2);
		verifyqueue.add(msg3);
		verifyqueue.process();

		// The validity callback resets the targetValidity. Check to make sure it was invoked.
		assertNull(msg1.targetvalidity);
		assertNull(msg2.targetvalidity);
		assertNull(msg3.targetvalidity);
	}	@Test
 
	public void testInsertAndProcessSimple() {
		insertAndProcess(1000, new SimpleQueue(new DigestPrimitive()));
    }
	
	@Test
    public void testInsertAndProcessMerkle() {
		insertAndProcess(1000, new MerkleQueue(new DigestPrimitive()));
    }

	@Test
    public void testInsertAndProcessHistory() {
		insertAndProcess(1000, new HistoryQueue(new DigestPrimitive()));
    }
	

	public void testInsertAndProcessSimpleTwice() {
		ProcessQueue queue = new SimpleQueue(new DigestPrimitive());
		insertAndProcess(1000, queue);
		insertAndProcess(2000, queue);
    }
	
	@Test
    public void testInsertAndProcessMerkleTwice() {
		ProcessQueue queue = new SimpleQueue(new DigestPrimitive());
		insertAndProcess(1000, queue);
		insertAndProcess(2000, queue);
    }

	@Test
    public void testInsertAndProcessHistoryTwice() {
		ProcessQueue queue = new SimpleQueue(new DigestPrimitive());
		insertAndProcess(1000, queue);
		insertAndProcess(2000, queue);
    }
}
