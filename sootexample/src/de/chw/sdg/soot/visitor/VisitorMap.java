package de.chw.sdg.soot.visitor;

import java.util.HashMap;

public class VisitorMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = -4735906021144491497L;

	private final Visitor<K, ?> visitor;

	public VisitorMap(Visitor<K, ?> visitor) {
		this.visitor = visitor;
	}

	//	@Override
	//	public Node put(K key, Node value) {
	//		return super.put(key, value);
	//	}

	public V getValue(K key) {
		V value = get(key);
		if (value == null) {
			visitor.visit(key);
			value = get(key);
		}
		return value;
	}
}
