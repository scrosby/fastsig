package edu.rice.historytree.aggs;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.rice.historytree.AggregationInterface;



@SuppressWarnings("unchecked")
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
			public AggregationInterface newInstance() { return new ConcatAgg();} 
		});
	}
}
