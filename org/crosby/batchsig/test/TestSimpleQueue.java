package org.crosby.batchsig.test;

import org.junit.Test;
import org.rice.crosby.batchsig.Message;
import org.rice.crosby.batchsig.SimpleQueue;
import org.rice.crosby.batchsig.VerifyQueue;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

import junit.framework.TestCase;


public class TestSimpleQueue extends TestCase {
	class MessageWrap implements Message {
		byte data[];
		TreeSigBlob signature;
		Boolean targetvalidity = null;
		
		public MessageWrap(int i) {
			data = String.format("Foo%d",i).getBytes(); 
		}

		@Override
		public byte[] getData() {
			return data;
		}

		@Override
		public Object getRecipient() {
			return new Object();
		}

		@Override
		public TreeSigBlob getSignatureBlob() {
			return this.signature;
		}

		@Override
		public Object getSigner() {
			return getClass();
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
	
	
	@Test 
	public void testInsertAndProcess() {
		SimpleQueue signqueue = new SimpleQueue(new DigestPrimitive());
		
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

		assertNull(msg1.targetvalidity);
		assertNull(msg2.targetvalidity);
		assertNull(msg3.targetvalidity);
	}
}
