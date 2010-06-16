package edu.rice.batchsig.bench;

import java.io.IOException;
import java.io.InputStream;


import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.VerifyQueue;
import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;


/** 
 * For benchmarking, we log a set of messages and signatures.
 * 
 * Take a bunch of messages and their signatures and replay them, running them through a queue.
 *  
 *  Message, sigblob. Message, sigblob. 
 *   */
public class SignLog implements Runnable {
	CodedInputStream input;
	private VerifyQueue queue;
	
	SignLog(CodedInputStream input, VerifyQueue queue) {
		this.input = input;
		this.queue = queue;
	}

	public void run() {
		Message message;
		while ((message = IncomingMessage.readFrom(input)) != null) {
			queue.add(message);
		}
	}
}
