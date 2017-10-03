package com.tnr.neo4j.java.nullobject;

import com.tnr.neo4j.java.nullobject.util.GraphDatabaseConstants;


/**
 * Modify the values in GraphDatabaseConstants to set up your own Database.
 * 
 * To Execute the transformation methods either uncomment them or call the execute() method
 * of a Transformation class.
 * @author Tim-Niklas Reck
 *
 */
public class Main {
	
	public static void main(String[] args) {
		Transformation transformation = new NullObjectTransformation(
				GraphDatabaseConstants.GraphDatabaseLocation, 
				GraphDatabaseConstants.cacheDB);
		
//		transformation.createIndexes();
//		test.printIndexes();
//		transformation.match();
//		transformation.transform();
		
		transformation.execute();
		
	}
}
