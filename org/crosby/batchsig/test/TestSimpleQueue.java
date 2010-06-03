package org.crosby.batchsig.test;

import org.junit.Test;
import org.rice.crosby.batchsig.HistoryQueue;
import org.rice.crosby.batchsig.MerkleQueue;
import org.rice.crosby.batchsig.Message;
import org.rice.crosby.batchsig.ProcessQueue;
import org.rice.crosby.batchsig.SimpleQueue;
import org.rice.crosby.batchsig.VerifyQueue;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

import junit.framework.TestCase;


public class TestSimpleQueue extends TestCase {
	class MessageWrap implements Message {
		byte data[];
		TreeSigBlob signature;
		Boolean targetvalidity = null;
		Object recipient;
		Object author;
		
		public MessageWrap(int i) {
			data = String.format("Foo%d",i).getBytes(); 
			recipient = this;
			author = getClass();
		}

		MessageWrap setRecipient(Object o) {
			recipient = o;
			return this;
		}

		MessageWrap setSigner(Object o) {
			author = o;
			return this;
		}
		
		@Override
		public byte[] getData() {
			return data;
		}

		@Override
		public Object getRecipient() {
			return recipient;
		}

		@Override
		public TreeSigBlob getSignatureBlob() {
			return this.signature;
		}

		@Override
		public Object getAuthor() {
			return author;
		}

		@Override
		public void signatureResult(TreeSigBlob sig) {
			//System.out.format("Signed '%s' with sig: {{%s}}\n" , new String(data) ,sig.toString());
			this.signature = sig;
		}

		@Override
		public void signatureValidity(boolean valid) {
			assertEquals(targetvalidity.booleanValue(),valid);
			targetvalidity = null;
		}

		public void wantValid() {targetvalidity = true;}
		public void wantInValid() {targetvalidity = false;}
	}

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
