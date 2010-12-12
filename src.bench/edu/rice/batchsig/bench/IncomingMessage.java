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
import java.util.concurrent.atomic.AtomicBoolean;


import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.lazy.VerifyHisttreeLazily;
import edu.rice.batchsig.lazy.VerifyLazily;
import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class IncomingMessage extends MessageBase implements IMessage {
	/** The time that this message was created. Used to get processing latency */
	private long creation_time = -1;
	public List<Integer> start_buffering;
	public List<Integer> end_buffering;
	int recipientuser;
	AtomicBoolean solved = new AtomicBoolean(false);
	
	// Sig = null occurs if there is no message data (aka, this is a non-signed messgae
	private IncomingMessage(TreeSigBlob sig, MessageData data) {
		//System.out.println("IncomingMsg Parse"+data+sig);
		this.sigblob = sig;
		if (data.hasMessage())
			this.data = data.getMessage().toByteArray();
		else
			this.data = null;
		//this.creation_time = System.currentTimeMillis();
		if (!data.hasTimestamp())
			throw new Error("Serialized messages must have timestamps");
		this.virtual_clock = data.getTimestamp();
		if (data.getStartBufferingUsersCount() > 0)
			this.start_buffering = data.getStartBufferingUsersList();
		if (data.getEndBufferingUsersCount() > 0)
			this.end_buffering = data.getEndBufferingUsersList();
		this.recipientuser = data.getRecipientUser();
		//System.out.print(this);
	}

	@Override
	public Object getAuthor() {
		// A ByteString is hashable and thus comparable and suitable to return.
		return sigblob.getSignerId();
	}

	@Override
	public Object getRecipientUser() {
		return recipientuser;
	}

	@Override
	public TreeSigBlob getSignatureBlob() {
		return sigblob;
	}

	public void resetCreationTimeToNow() {
		if (creation_time != -1)
			throw new Error("");
		creation_time = System.currentTimeMillis();
	}
	public void resetCreationTimeTo(long tstamp) {
		if (creation_time != -1)
			throw new Error("");
		creation_time = tstamp;
	}
	
	public void resetCreationTimeNull() {
		if (creation_time != -1)
			throw new Error("");
	}
	
	public long getCreationTime() {
		return creation_time;
	}
		
	@Override
	public void signatureValidity(boolean valid) {
		if (creation_time > 0)
			Tracker.singleton.trackLatency((int)(System.currentTimeMillis()- creation_time));
		// Runs in the validation thread, not the submit thread 
		Tracker.singleton.validated++;
		if (solved.compareAndSet(false, true)) {
			// Now do a callback into the verifier to count ourselves as done.
		} else {
			throw new Error("Can't report the same message twice");
		}
	
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
			
			//System.out.println("DATA"+data.toString());

			if (data.hasMessage()) {
				input.readMessage(sigbuilder, ExtensionRegistryLite.getEmptyRegistry());
				TreeSigBlob sig = sigbuilder.build();
				//System.out.println("SIG"+sig.toString());
				return new IncomingMessage(sig,data);
			} else {
				return new IncomingMessage(null,data);
			}
		} catch (IOException e) {
			System.err.println("readFrom stacktrace (returning null -- nonfatal error!)");
			e.printStackTrace();
			//throw new Error("ERROR",e);
			return null;
		}
	}

	public String toString() {
		if (getSignatureBlob()!= null && getRecipientUser() != null)
			return String.format("{time=%d, RecipientU=%s, leaf=%d at treeversion %d  %s}",
					getVirtualClock(), getRecipientUser(), getSignatureBlob().getLeaf(),
					getSignatureBlob().getTree().getVersion(),
					((ByteString)getAuthor()).toStringUtf8());
		else
			return String.format("{time=%d, RecipientU=%s, leaf=XXX at treeversion XXX  XXX}",
					getVirtualClock(), getRecipientUser());
	}
	
}
