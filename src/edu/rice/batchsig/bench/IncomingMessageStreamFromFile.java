package edu.rice.batchsig.bench;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;

import com.google.protobuf.CodedInputStream;

/** Generate an infinite stream of messages */
public class IncomingMessageStreamFromFile {
	private CodedInputStream input;
	final private FileInputStream fileinput;

	
	public IncomingMessageStreamFromFile(FileInputStream fileinput) {
		if (fileinput == null)
			throw new Error();
		this.fileinput = fileinput;
		resetStream();
	}
	
	public void resetStream() {
		try {
			fileinput.getChannel().position(0);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Cannot reset",e);
		}
		this.input = CodedInputStream.newInstance(fileinput);
	}
	
	/** Read the next message. Resetting the stream back to the beginning if we're at the end */
	public IncomingMessage next() {
		// Repeat until we get a good message.
		do {
			IncomingMessage msg = IncomingMessage.readFrom(input);
			// Bad message. Reset the stream and try again.
			//System.out.println("Reading message");
			input.resetSizeCounter();
			if (msg != null ) {
				return msg;
			}
			resetStream();
			System.out.println("Resetting stream");
		} while (true);		
	}
	
	/** Read the next message unconditionally, without resetting the stream to the beginning on end. */
	public IncomingMessage nextOnePass() {
		IncomingMessage m = IncomingMessage.readFrom(input);
		input.resetSizeCounter();
		return m;
	}
}
