/**
 * 
 */
package edu.rice.batchsig;


import edu.rice.batchsig.Message;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

/** Simple message creator that can store signatures and all the rest. */
class MessageWrap implements IMessage, OMessage {
	final byte data[];
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

	MessageWrap setAuthor(Object o) {
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
		System.out.format("Storing signature of '%s' with sig: {{%s}}\n" , new String(data) ,sig.toString());
		this.signature = sig;
	}

	@Override
	public void signatureValidity(boolean valid) {
		TestSimpleQueue.assertEquals(targetvalidity.booleanValue(),valid);
		targetvalidity = null;
	}

	public void wantValid() {targetvalidity = true;}
	public void wantInValid() {targetvalidity = false;}

	@Override
	public Object getRecipientUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetCreationTimeNull() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetCreationTimeTo(long tstamp) {
		// TODO Auto-generated method stub
		
	}
	

	
	
}