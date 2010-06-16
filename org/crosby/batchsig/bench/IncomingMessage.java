package org.crosby.batchsig.bench;

import java.io.IOException;

import org.rice.crosby.historytree.generated.Serialization.MessageData;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;

public class IncomingMessage extends MessageBase {
	private IncomingMessage(TreeSigBlob sig, MessageData data) {
		this.sigblob = sig;
		this.data = data.getMessage().toByteArray();
	}

	@Override
	public Object getAuthor() {
		// A ByteString is hashable and thus comparable and suitable to return.
		return sigblob.getSignerId();
	}

	@Override
	public Object getRecipient() {
		// TODO: Used when creating a message to be logged. Leaf unspecified for now.
		throw new Error("Unimplemented");
	}

	@Override
	public TreeSigBlob getSignatureBlob() {
		return sigblob;
	}

	@Override
	public void signatureResult(TreeSigBlob message) {
		// TODO: Used when creating a message to be logged. Leaf unspecified for now.
		throw new Error("Unimplemented");
	}

	@Override
	public void signatureValidity(boolean valid) {
		if (valid)
			System.out.println("Signature valid");
		else
			System.out.println("Signature failed");
	}

	static public IncomingMessage readFrom(CodedInputStream input) {
		try {
			MessageData.Builder databuilder = MessageData.newBuilder();
			TreeSigBlob.Builder sigbuilder= TreeSigBlob.newBuilder();
			input.readMessage(databuilder, ExtensionRegistryLite.getEmptyRegistry());
			MessageData data = databuilder.build();
			// No data means we're done.
			if (data.getMessage().isEmpty())
				return null;
			input.readMessage(sigbuilder, ExtensionRegistryLite.getEmptyRegistry());
			TreeSigBlob sig = sigbuilder.build();
			return new IncomingMessage(sig,data);
	} catch (IOException e) {
		return null;
	}
}

}
