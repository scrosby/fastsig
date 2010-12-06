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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.OMessage;
import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class OutgoingMessage extends MessageBase implements OMessage {
	CodedOutputStream output;
	Object recipient_user;
	Object recipient;
	long creation_time;
	List<Integer> logins, logouts;
	
	
	public OutgoingMessage(CodedOutputStream output, byte data[], Object recipient, Object recipient_user) {
		this.output = output;
		this.data = data;
		this.recipient = recipient;
		this.recipient_user = recipient_user;
		this.creation_time = System.currentTimeMillis();
	}

	public void setLoginsLogouts(List<Integer> logins, List<Integer> logouts) {
		this.logins = logins;
		this.logouts = logouts;
	}
	
	
	
	@Override
	public Object getRecipient() {
		return recipient;
	}

	@Override
	public void signatureResult(TreeSigBlob sigblob) {
		this.sigblob = sigblob;
		try {
			track();
			if (output != null) {
				writeTo(output);
				output.flush();
				}
			} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void track() {
		Tracker tracker = Tracker.singleton;
		if (tracker != null) {
			tracker.trackLatency((int)(System.currentTimeMillis() - creation_time));
			//System.out.println(this.sigblob.toString());
			tracker.trackMsgBytesize(this.sigblob.getSerializedSize());
		}
	}

	public void writeTo(CodedOutputStream output) throws IOException {
		if (output != this.output) {
			throw new Error("Inconsistent output?");
		}
		List<Integer> startBuffering = this.logouts;
		List<Integer> endBuffering = this.logins;
		
		MessageData.Builder builder = MessageData.newBuilder();
		
		builder.setTimestamp(virtual_clock);
		if (data != null) {
			builder.setMessage(ByteString.copyFrom(data));
			builder.setRecipientUser((Integer)recipient_user);
		} else if (logins == null && logouts == null)
			throw new Error("No data?");
		
		if (startBuffering != null || endBuffering != null) {
			builder.addAllStartBufferingUsers(startBuffering);
			builder.addAllEndBufferingUsers(endBuffering);
		}
		
		
		MessageData messagedata = builder.build();
		
		//System.out.println("MSGData:"+messagedata);
		//System.out.println("SigBlob:"+sigblob);

		if (messagedata.getSerializedSize() > 1000000) {
			System.out.println(messagedata);
			throw new Error("Unexpectedly big message");
		}
		output.writeRawVarint32(messagedata.getSerializedSize());
		messagedata.writeTo(output);
		
		if (data != null) {
			if (sigblob.getSerializedSize() > 1000000) {
				System.out.println(sigblob);
				throw new Error("Unexpectedly big message");
			}
			output.writeRawVarint32(sigblob.getSerializedSize());
			//System.out.println("SigBlob:"+sigblob);
			sigblob.writeTo(output);
		}
		output.flush();
		
	}

	/*
	public void writeTo(CodedOutputStream output) throws IOException {
		if (data == null)
			throw new Error("Simple writer does not support empty message bodies");
		//System.out.println("Writing");
		MessageData messagedata = MessageData.newBuilder()
			.setTimestamp(virtual_clock)
			.setMessage(ByteString.copyFrom(data))
			.build();

		output.writeRawVarint32(messagedata.getSerializedSize());
		messagedata.writeTo(output);
		
		output.writeRawVarint32(sigblob.getSerializedSize());
		sigblob.writeTo(output);
	}
	*/
}
