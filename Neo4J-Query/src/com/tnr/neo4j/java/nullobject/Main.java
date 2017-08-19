package com.tnr.neo4j.java.nullobject;

public class Main {
	
	public static void main(String[] args) {
		Transform2NullObject test = new Transform2NullObject();
		test.createIndexes();
//		test.getIndexes();
		test.match();
		test.transform();
	}
}
