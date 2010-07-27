/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.batchsig.bench;

import java.io.IOException;


import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;

import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class IncomingMessage extends MessageBase {
	private Tracker tracker;
	/** The time that this message was created. Used to get processing latency */
	private long creation_time;

	private IncomingMessage(TreeSigBlob sig, MessageData data, double virtual_clock, Tracker tracker) {
		this.sigblob = sig;
		this.data = data.getMessage().toByteArray();
		this.creation_time = System.currentTimeMillis();
		this.virtual_clock = virtual_clock;
		this.tracker = tracker;
	}

	@Override
	public Object getAuthor() {
		// A ByteString is hashable and thus comparable and suitable to return.
		return sigblob.getSignerId();
	}

	@Override
	public Object getRecipient() {
		// TODO: Used when creating a message to be logged. Leave unspecified for now.
		throw new Error("Unimplemented");
	}

	@Override
	public TreeSigBlob getSignatureBlob() {
		return sigblob;
	}

	@Override
	public void signatureResult(TreeSigBlob message) {
		// TODO: Used when creating a message to be logged. Leave unspecified for now.
		throw new Error("Unimplemented");
	}

	@Override
	public void signatureValidity(boolean valid) {
		tracker.trackLatency((int)(System.currentTimeMillis()- creation_time));
		/*
		if (valid)
			System.out.println("Signature valid");
		else
			System.out.println("Signature failed");
	*/
	}

	static public IncomingMessage readFrom(CodedInputStream input, Tracker tracker) {
		try {
			MessageData.Builder databuilder = MessageData.newBuilder();
			TreeSigBlob.Builder sigbuilder= TreeSigBlob.newBuilder();
			double virtual_clock = input.readDouble();
			
			input.readMessage(databuilder, ExtensionRegistryLite.getEmptyRegistry());
			MessageData data = databuilder.build();
			// No data means we're done.
			if (data.getMessage().isEmpty())
				return null;
			input.readMessage(sigbuilder, ExtensionRegistryLite.getEmptyRegistry());
			TreeSigBlob sig = sigbuilder.build();
			return new IncomingMessage(sig,data,virtual_clock,tracker);
	} catch (IOException e) {
		return null;
	}
}

}
