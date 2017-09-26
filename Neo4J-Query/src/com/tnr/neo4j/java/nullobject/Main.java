package com.tnr.neo4j.java.nullobject;

import com.tnr.neo4j.java.nullobject.util.Constants;

public class Main {
	
	public static void main(String[] args) {
		Transformation test = new NullObjectTransformation(
				Constants.GraphDatabaseLocation, 
				Constants.cacheDB);
		test.createIndexes();
//		test.printIndexes();
		test.match();
		test.transform();
	}
}
