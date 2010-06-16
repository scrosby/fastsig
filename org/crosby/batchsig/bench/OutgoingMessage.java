package org.crosby.batchsig.bench;

import java.io.IOException;

import org.rice.crosby.historytree.generated.Serialization.MessageData;
import org.rice.crosby.historytree.generated.Serialization.TreeSigBlob;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

public class OutgoingMessage extends MessageBase {
	CodedOutputStream output;
	Object recipient;
	
	public OutgoingMessage(CodedOutputStream output, byte data[], Object recipient) {
		this.data = data;
		this.recipient = recipient;
	}
	
	@Override
	public Object getAuthor() {
		throw new Error("Unimplemented");
	}

	@Override
	public Object getRecipient() {
		return recipient;
	}

	@Override
	public TreeSigBlob getSignatureBlob() {
		throw new Error("Unimplemented");
	}

	@Override
	public void signatureResult(TreeSigBlob sigblob) {
		this.sigblob = sigblob;
		try {
			write();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void signatureValidity(boolean valid) {
		// TODO Auto-generated method stub

	}

	void write() throws IOException {
		MessageData messagedata = MessageData.newBuilder().setMessage(ByteString.copyFrom(data)).build();
		output.writeRawVarint32(messagedata.getSerializedSize());
		messagedata.writeTo(output);
		
		output.writeRawVarint32(sigblob.getSerializedSize());
		sigblob.writeTo(output);
	}
}
