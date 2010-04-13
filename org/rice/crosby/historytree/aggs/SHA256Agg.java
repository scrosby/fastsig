package org.rice.crosby.historytree.aggs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class SHA256Agg extends HashAggBase {
	public MessageDigest getAlgo(byte tag) {
		try {
			MessageDigest md=MessageDigest.getInstance("SHA256");
			md.update(tag);
			return md;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public String getName() {
		return "SHA256Agg";
	}
}
