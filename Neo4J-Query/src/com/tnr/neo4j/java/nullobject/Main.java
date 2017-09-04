package com.tnr.neo4j.java.nullobject;

public class Main {
	
	public static void main(String[] args) {
		NullObjectTransformation test = new NullObjectTransformation();
		test.createIndexes();
//		test.getIndexes();
		test.match();
		test.transform2nullObject();
	}
}
