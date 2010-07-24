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
import java.io.InputStream;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.Message;
import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

abstract public class MessageBase implements Message{
	double timestamp;
	protected TreeSigBlob sigblob;
	protected byte [] data;

	public MessageBase() {
		super();
	}

	@Override
	public byte[] getData() {
		return data;
	}

	public double getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(double d) {
		this.timestamp = d;
	}

	void writeTo(CodedOutputStream output) throws IOException {
		MessageData messagedata = MessageData.newBuilder().setMessage(ByteString.copyFrom(data)).build();
		output.writeRawVarint32(messagedata.getSerializedSize());
		messagedata.writeTo(output);
		
		output.writeRawVarint32(sigblob.getSerializedSize());
		sigblob.writeTo(output);
	}

	void readFrom(InputStream input) throws IOException {
		MessageData.Builder messagedatabuilder = MessageData.newBuilder();
		messagedatabuilder.mergeDelimitedFrom(input);
		TreeSigBlob.Builder sigblobbuilder = TreeSigBlob.newBuilder();
		sigblobbuilder.mergeDelimitedFrom(input);
		sigblob = sigblobbuilder.build();
		data = messagedatabuilder.build().getMessage().toByteArray();
		}
}