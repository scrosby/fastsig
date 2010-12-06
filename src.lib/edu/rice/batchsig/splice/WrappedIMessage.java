package edu.rice.batchsig.splice;

import edu.rice.batchsig.IMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class WrappedIMessage implements IMessage {
	final IMessage o;
	private Callback validator;
	
	public interface Callback {
		public void messageValidatorCallback(IMessage o, boolean valid);
	}
	

	WrappedIMessage(IMessage o) {
		this.o = o;
	}
	
	@Override
	public byte[] getData() {
		return o.getData();
	}

	@Override
	public void signatureValidity(boolean valid) {
		o.signatureValidity(valid);
		if (validator != null)
			validator.messageValidatorCallback(o,valid);
	}

	@Override
	public TreeSigBlob getSignatureBlob() {
		return o.getSignatureBlob();
	}

	@Override
	public Object getAuthor() {
		return o.getAuthor();
	}

	@Override
	public Object getRecipientUser() {
		return o.getRecipientUser();
	}

	public void setCallback(Callback c) {
		validator = c;
	}
}
