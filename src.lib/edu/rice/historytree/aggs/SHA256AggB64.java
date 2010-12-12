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


import com.google.protobuf.ByteString;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import edu.rice.historytree.AggregationInterface;

/** Extend the prior standard SHA256Agg class to print human readable base64'ed values */
@SuppressWarnings("rawtypes")
public class SHA256AggB64 extends SHA256Agg {
	@Override
	public byte[] parseAgg(ByteString b) {
		try {
			return Base64.decode(b.toByteArray());
		} catch (Base64DecodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ByteString serializeAgg(byte[] agg) {
		return ByteString.copyFromUtf8(Base64.encode(agg));
	}

	@Override
	public String getName() {
		return NAME;
	}
	static final String NAME = "SHA256AggB64";
	static { 
		AggRegistry.register(new AggregationInterface.Factory() {
			public String name() {return NAME;}
			public AggregationInterface newInstance() { return new SHA256AggB64();} 
		});
	}
}
