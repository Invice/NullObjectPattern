package com.tnr.neo4j.java.example;

import java.io.File;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


public class JavaNeo4JTest {
	
	public static final Label TestLabel1 = Label.label("1stTestNode");
	public static final Label TestLabel2 = Label.label("2ndTestNode");
	public enum RelTypes implements RelationshipType {
		KNOWS, KNOWS_NOT;
	}
			
	public static final String matchQuery = "MATCH (mainClass:Class)-[:CONTAINS_FIELD]->(candidateField:Field)\nMATCH j=((method:Method) -[:CONTROL_FLOW]->(this:Assignment {operation:\"thisdeclaration\"})-[:CONTROL_FLOW*]->(ifCond:NopStmt {nopkind:\"IF_COND\"})-[:CONTROL_FLOW]->(tmpCond:Assignment)-[:CONTROL_FLOW]->(condEval:Condition {operation:\"!=\"})-[:CONTROL_FLOW]->(:NopStmt {nopkind:\"IF_THEN\"})-[:CONTROL_FLOW*]->(candidateCall:Assignment)-[:CONTROL_FLOW*]->(ifEnd:NopStmt {nopkind:\"IF_END\"})) \n\u0009WHERE (method)-[:CONTAINS_UNIT]->(this)\u0009\n\u0009\u0009AND (this)-[:DATA_FLOW]->(tmpCond)-[:DATA_FLOW]->(condEval) \n\u0009\u0009AND (candidateField)-[:DATA_FLOW ]->(tmpCond) \n\u0009\u0009AND (condEval.operand1=\"null\" OR condEval.operand2=\"null\")\nMATCH k=((condEval)-[:CONTROL_FLOW]->(:NopStmt {nopkind:\"IF_ELSE\"})-[:CONTROL_FLOW*]->(ifEnd))\nMATCH (candidate:Class)\n\u0009WHERE candidateField.vartype = candidate.fqn\nRETURN DISTINCT mainClass, candidateField, j, k, candidate";
	public static final String matchQuery2 = "MATCH (mainClass:Class)-[:CONTAINS_FIELD]->(candidateField:Field)\nMATCH j=((method:Method) -[:CONTROL_FLOW]->(this:Assignment {operation:\"thisdeclaration\"})-[:CONTROL_FLOW*]->(ifCond:NopStmt {nopkind:\"IF_COND\"})-[:CONTROL_FLOW]->(tmpCond:Assignment)-[:CONTROL_FLOW]->(condEval:Condition {operation:\"!=\"})-[:CONTROL_FLOW]->(:NopStmt {nopkind:\"IF_THEN\"})-[:CONTROL_FLOW*]->(candidateCall:Assignment)-[:CONTROL_FLOW*]->(ifEnd:NopStmt {nopkind:\"IF_END\"})) \n\u0009WHERE (method)-[:CONTAINS_UNIT]->(this)\u0009\n\u0009\u0009AND (this)-[:DATA_FLOW]->(tmpCond)-[:DATA_FLOW]->(condEval) \n\u0009\u0009AND (candidateField)-[:DATA_FLOW ]->(tmpCond) \n\u0009\u0009AND (condEval.operand1=\"null\" OR condEval.operand2=\"null\")\nMATCH k=((condEval)-[:CONTROL_FLOW]->(:NopStmt {nopkind:\"IF_ELSE\"})-[:CONTROL_FLOW*]->(ifEnd))\nMATCH (package:Package) --> (candidate:Class)\n\u0009WHERE candidateField.vartype = candidate.fqn\nRETURN candidate, candidateField, package";
	
	public static void main(String[] args) {
		GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
		GraphDatabaseService db = dbFactory.newEmbeddedDatabase(new File("C:\\Users\\Tim-Niklas Reck\\Desktop\\Bachelorarbeit\\sootexample\\databases\\de.tnr.sdg.example.cache.MainClass"));
		
//		try (Transaction tx = db.beginTx()){
//			
//			Node testNode = db.createNode(JavaNeo4JTest.TestLabel1);
//			testNode.setProperty("type", "TestWert");
//			
//			Node testNode2 = db.createNode(JavaNeo4JTest.TestLabel2);
//			testNode2.setProperty("type", "TestWert2");	
//			
//			Relationship relationship = testNode.createRelationshipTo(testNode2, RelTypes.KNOWS);
//			relationship.setProperty("id", "1");
//			tx.success();
//		}
		
		try (Result queryResult =  db.execute(JavaNeo4JTest.matchQuery))
		{
			
			System.out.println(queryResult.resultAsString());
			
			
			
			
			
//		     while ( queryResult.hasNext() )
//		     {
//		         Map<String, Object> row = queryResult.next();
//		         for(String column :queryResult.columns()){
//		        	 System.out.println(column);
//		         }
//		         System.out.println();
//		         
//		         for ( String key : queryResult.columns() )
//		         {
//		             System.out.printf( "%s = %s%n", key, row.get( key ) );
//		         }
//		     }
		 }
		
		
		
		
//		System.out.println("Done successfully");
		
	}

}
