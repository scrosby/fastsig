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
import java.util.List;


import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;

import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class IncomingMessage extends MessageBase {
	/** The time that this message was created. Used to get processing latency */
	private long creation_time;
	public List<Integer> start_buffering;
	public List<Integer> end_buffering;

	// Sig = null occurs if there is no message data (aka, this is a non-signed messgae
	private IncomingMessage(TreeSigBlob sig, MessageData data) {
		this.sigblob = sig;
		this.data = data.getMessage().toByteArray();
		this.creation_time = System.currentTimeMillis();
		this.virtual_clock = data.getTimestamp();
		if (data.getStartBufferingCount() > 0)
			this.start_buffering = data.getStartBufferingList();
		if (data.getEndBufferingCount() > 0)
			this.end_buffering = data.getEndBufferingList();
		//this.recipientuser = data.
	}

	@Override
	public Object getAuthor() {
		// A ByteString is hashable and thus comparable and suitable to return.
		return sigblob.getSignerId();
	}

	@Override
	public Object getRecipient() {
		// TODO: Used when creating a message to be logged. Illegal on incomming messages.
		throw new Error("Illegal Operation");
	}

	public Object getRecipientUser() {
		throw new Error("TODO"); // TODO 
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
		Tracker.singleton.trackLatency((int)(System.currentTimeMillis()- creation_time));
		/*
		if (valid)
			System.out.println("Signature valid");
		else
			System.out.println("Signature failed");
	*/
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

			if (data.hasMessage()) {
				input.readMessage(sigbuilder, ExtensionRegistryLite.getEmptyRegistry());
				TreeSigBlob sig = sigbuilder.build();
				return new IncomingMessage(sig,data);
			} else {
				return new IncomingMessage(null,data);
			}
		} catch (IOException e) {
		return null;
	}	
	}

}