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
import java.security.NoSuchAlgorithmException;

import edu.rice.historytree.AggregationInterface;



@SuppressWarnings("rawtypes")
public class SHA256Agg extends HashAggBase {
	public MessageDigest getAlgo(byte tag) {
		try {
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			md.update(tag);
			return md;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	static final String NAME = "SHA256Agg";
	static { 
		AggRegistry.register(new AggregationInterface.Factory() {
			public String name() {return NAME;}
			public AggregationInterface newInstance() { return new SHA256Agg();} 
		});
	}
}
