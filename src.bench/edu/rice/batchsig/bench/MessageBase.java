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

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.Message;
import edu.rice.batchsig.OMessage;
import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

abstract public class MessageBase {
	protected TreeSigBlob sigblob;
	protected byte [] data;
	/** Contains a virtual clock time, for simulations */
	protected long virtual_clock = -3;
	
	public MessageBase() {
		super();
	}

	public byte[] getData() {
		return data;
	}

	public long getVirtualClock() {
		return virtual_clock;
	}
	public void setVirtualClock(long d) {
		this.virtual_clock = d;
	}

}