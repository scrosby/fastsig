package org.rice.crosby.historytree.aggs;

import java.security.MessageDigest;

import org.rice.crosby.historytree.AggregationInterface;

import com.google.protobuf.ByteString;

abstract public class HashAggBase implements AggregationInterface<byte[], byte[]> {

	abstract public MessageDigest getAlgo(byte tag);

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