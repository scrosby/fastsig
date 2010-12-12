package edu.rice.batchsig.lazy;

import edu.rice.batchsig.IMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

/**
 * Wrap an IMessage with one that is the same, except that it supports an extra
 * callback on whether the message is valid or not. I need to do this because,
 * with spliced signatures, messages can validate 'by surprise' without being
 * asked to. The lazy verifier needs to know if this has occurred so that it can
 * cease tracking the message. Ergo, it wraps messages so-as to receive these
 * notifications.
 */
public class WrappedIMessage implements IMessage {
	/** The message being wrapped. */
	final private IMessage o;
	/** The callback. */
	private Callback validator;

	/** The interface that the class wanting the callback must support. */
	public interface Callback {
		/** @see edu.rice.batchsig.IMessage#signatureValidity */
		void messageValidatorCallback(IMessage o, boolean valid);
	}

	/** Constructor */
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
			validator.messageValidatorCallback(o, valid);
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

	/** Set the callback object to be invoked. */
	public void setCallback(Callback c) {
		validator = c;
	}

	@Override
	public void resetCreationTimeNull() {
		o.resetCreationTimeNull();
	}

	@Override
	public void resetCreationTimeTo(long tstamp) {
		o.resetCreationTimeTo(tstamp);
	}
}
