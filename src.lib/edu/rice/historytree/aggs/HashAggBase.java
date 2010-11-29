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

package edu.rice.historytree.aggs;

import java.security.MessageDigest;


import com.google.protobuf.ByteString;

import edu.rice.historytree.AggregationInterface;

abstract public class HashAggBase implements AggregationInterface<byte[], byte[]> {

	abstract public MessageDigest getAlgo(byte tag);

	@Override
	public byte[] emptyAgg() {
		final byte empty[] = {0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0};
		return empty;
	}
			
	@Override
	public String getConfig() {
		return "";
	}

	@Override
	public byte[] parseAgg(ByteString b) {
		return b.toByteArray();
	}

	@Override
	public byte[] parseVal(ByteString b) {
		return b.toByteArray();
	}

	@Override
	public ByteString serializeAgg(byte[] agg) {
		return ByteString.copyFrom(agg);
	}

	@Override
	public ByteString serializeVal(byte[] val) {
		return ByteString.copyFrom(val);
	}

	@Override
	public AggregationInterface<byte[], byte[]> setup(String config) {
		return this;
	}
	@Override
	public byte[] aggChildren(byte[] leftAnn, byte[] rightAnn) {
		if (rightAnn != null) {
			//System.out.println("AC: " + serializeAgg(leftAnn).toStringUtf8() + "  " + serializeAgg(rightAnn).toStringUtf8()); 
			MessageDigest md=getAlgo((byte)1);
			md.update(leftAnn);
			md.update(rightAnn);
			return md.digest();
		} else {
			//System.out.println("AC: " + serializeAgg(leftAnn).toStringUtf8() + "  __________________");
			MessageDigest md=getAlgo((byte)2);
			md.update(leftAnn);
			return md.digest();
		}
	}
	@Override
	public byte[] aggVal(byte[] event) {
		return getAlgo((byte)0).digest(event);
	}
	@Override
	public AggregationInterface<byte[], byte[]> clone() {
		// ConcatAgg is stateless, so just return this
		return this;
	}

}