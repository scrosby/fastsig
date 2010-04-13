package org.rice.crosby.historytree.aggs;

import java.util.HashMap;

import org.rice.crosby.historytree.AggregationInterface;

public class AggRegistry {
	static HashMap<String, AggregationInterface.Factory> registry = new HashMap<String,AggregationInterface.Factory>();

	static void register(AggregationInterface.Factory factory) {
		registry.put(factory.name(),factory);
	}

	static AggregationInterface newInstance(String name) {
		return registry.get(name).newInstance();
	}
}
