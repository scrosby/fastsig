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

import edu.rice.historytree.AggregationInterface;

@SuppressWarnings("rawtypes")
public class ConcatAgg implements AggregationInterface<String, String> {
	@Override
	public String aggChildren(String leftAnn, String rightAnn) {
		StringBuilder out = new StringBuilder();
		out.append("[");
		out.append(leftAnn);
		out.append(",");
		if (rightAnn != null)
			out.append(rightAnn);
		out.append("]");
		return out.toString();
	}

	@Override
	public String aggVal(String event) {
		return event.substring(0,1);
	}

	@Override
	public String emptyAgg() {
		return "<>";
	}
	
	@Override
	public String getConfig() {
		return "";
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String parseAgg(ByteString b) {
		return b.toStringUtf8();
	}

	@Override
	public String parseVal(ByteString b) {
		return b.toStringUtf8();
	}

	@Override
	public ByteString serializeAgg(String agg) {
		return ByteString.copyFrom(agg.getBytes());
	}

	@Override
	public ByteString serializeVal(String val) {
		return ByteString.copyFrom(val.getBytes());
	}

	@Override
	public AggregationInterface<String, String> setup(String config) {
		return this;
	}

	@Override
	public AggregationInterface<String, String> clone() {
		// ConcatAgg is stateless, so just return this
		return this;
	}

	static final String NAME = "ConcatAgg";
	static { 
		AggRegistry.register(new AggregationInterface.Factory() {
			public String name() {return NAME;}
			public AggregationInterface newInstance() { return new ConcatAgg();} 
		});
	}
}
