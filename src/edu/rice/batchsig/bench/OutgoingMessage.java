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


import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class OutgoingMessage extends MessageBase {
	CodedOutputStream output;
	Object recipient;
	LatencyTracker tracker;
	long timestamp;
	
	public OutgoingMessage(LatencyTracker tracker, CodedOutputStream output, byte data[], Object recipient) {
		this.tracker = tracker;
		this.output = output;
		this.data = data;
		this.recipient = recipient;
		this.timestamp = System.currentTimeMillis();
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
			track();
			if (output != null)
				writeTo(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void signatureValidity(boolean valid) {
		// TODO Auto-generated method stub
	}
	
	void track() {
		if (tracker != null) {
			tracker.add((int)(System.currentTimeMillis() - timestamp));
			tracker.msgSize(this.sigblob.getSerializedSize());
		}
	}
	

}
