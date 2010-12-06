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

package edu.rice.batchsig.bench.log;

import java.io.IOException;
import java.io.InputStream;


import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;

import edu.rice.batchsig.IMessage;
import edu.rice.batchsig.Message;
import edu.rice.batchsig.VerifyQueue;
import edu.rice.batchsig.bench.IncomingMessage;
import edu.rice.historytree.generated.Serialization.MessageData;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;


/** 
 * For benchmarking, we log a set of messages and signatures.
 * 
 * Take a bunch of messages and their signatures and replay them, running them through a queue.
 *  
 *  Message, sigblob. Message, sigblob. 
 *     UM.... ??? BROKEN/UNUSED?
 *   
 *   */
public class SignLog implements Runnable {
	CodedInputStream input;
	private VerifyQueue queue;
	
	SignLog(CodedInputStream input, VerifyQueue queue) {
		this.input = input;
		this.queue = queue;
	}

	public void run() {
		IMessage message;
		while ((message = IncomingMessage.readFrom(input)) != null) {
			queue.add(message);
		}
	}
}
