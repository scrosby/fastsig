package org.rice.crosby.historytree.aggs;

import com.google.protobuf.ByteString;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

/** Extend the prior standard SHA256Agg class to be human readable base64'ed values */
public class SHA256AggB64 extends SHA256Agg {
	@Override
	public byte[] parseAgg(ByteString b) {
		try {
			return Base64.decode(b.toByteArray());
		} catch (Base64DecodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ByteString serializeAgg(byte[] agg) {
		return ByteString.copyFromUtf8(Base64.encode(agg));
	}
}
