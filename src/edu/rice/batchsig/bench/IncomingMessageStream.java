package edu.rice.batchsig.bench;

import java.io.FileInputStream;
import java.io.IOException;

import com.google.protobuf.CodedInputStream;

/** Generate an infinite stream of messages */
public class IncomingMessageStream {
	private CodedInputStream input;
	final private FileInputStream fileinput;

	
	public IncomingMessageStream(FileInputStream fileinput) {
		if (fileinput == null)
			throw new Error();
		this.fileinput = fileinput;
		resetStream();
	}
	
	public void resetStream() {
		try {
			fileinput.getChannel().position(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.input = CodedInputStream.newInstance(fileinput);
	}
	
	/** Read the next message. Resetting the stream back to the beginning if we're at the end */
	public IncomingMessage next() {
		// Repeat until we get a good message.
		do {
			IncomingMessage msg = IncomingMessage.readFrom(input);
			// Bad message. Reset the stream and try again.
			if (msg != null ) {
				return msg;
			}
			resetStream();
		} while (true);		
	}
	public IncomingMessage nextOnePass() {
		return IncomingMessage.readFrom(input);
	}
}
