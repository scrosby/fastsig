package org.crosby.batchsig.test;

import org.junit.Test;
import org.rice.crosby.batchsig.HistoryQueue;
import org.rice.crosby.batchsig.VerifyQueue;

import junit.framework.TestCase;


public class TestHistoryQueue extends TestCase {

	@Test
	/** Make sure splicepoints are properly made */
	public void testSplicepoints() {
		DigestPrimitive prims = new DigestPrimitive();
		HistoryQueue signqueue=new HistoryQueue(prims);
		
		Object target1 = new Object();
		Object target2 = new Object();
		Object target3 = new Object();
		Object target4 = new Object();

		MessageWrap msg0 = new MessageWrap(1000).setRecipient(target1);
		MessageWrap msg1 = new MessageWrap(1001).setRecipient(target1);
		MessageWrap msg2 = new MessageWrap(1002).setRecipient(target2);
		MessageWrap msg3 = new MessageWrap(1003).setRecipient(target3);
		MessageWrap msg4 = new MessageWrap(1004).setRecipient(target2);
		MessageWrap msg5 = new MessageWrap(1005).setRecipient(target1);

		MessageWrap msg6 = new MessageWrap(1006).setRecipient(target3);
		MessageWrap msg7 = new MessageWrap(1007).setRecipient(target2);
		MessageWrap msg8 = new MessageWrap(1008).setRecipient(target4);
		MessageWrap msg9 = new MessageWrap(1009).setRecipient(target2);

		signqueue.add(msg0);
		signqueue.add(msg1);
		signqueue.add(msg2);
		signqueue.add(msg3);
		signqueue.add(msg4);
		signqueue.add(msg5);
		signqueue.process();
		assertEquals(1,prims.signcount);
		
		assertEquals(msg0.getSignatureBlob().getLeaf(),0);
		assertEquals(msg1.getSignatureBlob().getLeaf(),1);
		assertEquals(msg5.getSignatureBlob().getLeaf(),5);

		//System.out.println(msg1.getSignatureBlob());
		//System.out.println(msg2.getSignatureBlob());

		assertEquals(0,msg0.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg1.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg2.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg3.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg4.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg5.getSignatureBlob().getSpliceHintCount());

		//System.out.println(msg0.getSignatureBlob());
		
		signqueue.add(msg6);
		signqueue.add(msg7);
		signqueue.add(msg8);
		signqueue.add(msg9);
		signqueue.process();
		assertEquals(2,prims.signcount);

		assertEquals(msg6.getSignatureBlob().getLeaf(),6);
		assertEquals(msg9.getSignatureBlob().getLeaf(),9);

		assertEquals(5,msg6.getSignatureBlob().getSpliceHint(0));
		assertEquals(5,msg7.getSignatureBlob().getSpliceHint(0));
		assertEquals(0,msg8.getSignatureBlob().getSpliceHintCount());
		assertEquals(0,msg9.getSignatureBlob().getSpliceHintCount());
	}

	public void testVerify() {
		DigestPrimitive prims = new DigestPrimitive();
		HistoryQueue signqueue=new HistoryQueue(prims);
		
		Object target1 = new Object();
		Object target2 = new Object();
		Object target3 = new Object();
		Object target4 = new Object();

		MessageWrap msg0 = new MessageWrap(1000).setRecipient(target1);
		MessageWrap msg1 = new MessageWrap(1001).setRecipient(target1);
		MessageWrap msg2 = new MessageWrap(1002).setRecipient(target2);
		MessageWrap msg3 = new MessageWrap(1003).setRecipient(target3);
		MessageWrap msg4 = new MessageWrap(1004).setRecipient(target2);
		MessageWrap msg5 = new MessageWrap(1005).setRecipient(target1);

		MessageWrap msg6 = new MessageWrap(1006).setRecipient(target3);
		MessageWrap msg7 = new MessageWrap(1007).setRecipient(target2);
		MessageWrap msg8 = new MessageWrap(1008).setRecipient(target4);
		MessageWrap msg9 = new MessageWrap(1009).setRecipient(target1);

		signqueue.add(msg0);
		signqueue.add(msg1);
		signqueue.add(msg2);
		signqueue.add(msg3);
		signqueue.add(msg4);
		signqueue.add(msg5);
		signqueue.process();

		signqueue.add(msg6);
		signqueue.add(msg7);
		signqueue.add(msg8);
		signqueue.add(msg9);
		signqueue.process();

		// Now try to verify them, in one batch.
		System.out.println("***** Verify Pass 1 *****");
		VerifyQueue verifyqueue=new VerifyQueue(prims);
		
		msg0.wantValid(); verifyqueue.add(msg0);
		msg1.wantValid(); verifyqueue.add(msg1);
		msg2.wantValid(); verifyqueue.add(msg2);
		msg3.wantValid(); verifyqueue.add(msg3);
		msg4.wantValid(); verifyqueue.add(msg4);
		msg5.wantValid(); verifyqueue.add(msg5);
		msg6.wantValid(); verifyqueue.add(msg6);
		msg7.wantValid(); verifyqueue.add(msg7);
		msg8.wantValid(); verifyqueue.add(msg8);
		msg9.wantValid(); verifyqueue.add(msg9);
		verifyqueue.process();
		assertEquals(1,prims.verifycount); // Only one tree-ID.
		
		// Now try to verify them, in more than one batch.
		System.out.println("***** Verify Pass 2 *****");
		prims.reset();
		assertEquals(0,prims.verifycount); // One for msg8, to a new recipient.
		verifyqueue=new VerifyQueue(prims);
		
		msg0.wantValid(); verifyqueue.add(msg0);
		msg1.wantValid(); verifyqueue.add(msg1);
		msg2.wantValid(); verifyqueue.add(msg2);
		msg3.wantValid(); verifyqueue.add(msg3);
		msg4.wantValid(); verifyqueue.add(msg4);
		verifyqueue.process();
		assertEquals(2,prims.verifycount); // One for msg8, to a new recipient.
		
		// Corrupt message 5.
		msg5.data[1]=0;
	
		msg5.wantInValid(); verifyqueue.add(msg5);
		msg6.wantValid(); verifyqueue.add(msg6);
		msg7.wantValid(); verifyqueue.add(msg7);
		msg8.wantValid(); verifyqueue.add(msg8);
		msg9.wantValid(); verifyqueue.add(msg9);
		verifyqueue.process();
	}





}
