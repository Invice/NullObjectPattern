package de.chw.sdg.db.transformer.util;

import soot.SootField;
import soot.SootMethod;

public class Aggregate {

	private final SootMethod method;
	private final SootField field;

	public int amount;

	public Aggregate(final SootMethod method, final SootField field) {
		super();
		this.method = method;
		this.field = field;
	}

	public SootMethod getMethod() {
		return method;
	}

	public SootField getField() {
		return field;
	}

}
