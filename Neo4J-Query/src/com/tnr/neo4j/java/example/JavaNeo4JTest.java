package com.tnr.neo4j.java.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.cypher.internal.compiler.v3_2.commands.indexQuery;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Resource;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import com.tnr.neo4j.java.util.Constants;
import com.tnr.neo4j.java.util.StringUtil;


public class JavaNeo4JTest {
	
	private GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
	private GraphDatabaseService db = dbFactory.newEmbeddedDatabase(new File(Constants.GraphDatabaseLocation + Constants.GraphDatabaseName));
	private IndexManager indexManager = db.index();
			
	private enum RelTypes implements RelationshipType {
		EXTENDS, CONTAINS_TYPE, CONTAINS_METHOD, DATA_FLOW, CONTROL_FLOW
	}
	
	public static final String matchQuery = "MATCH (mainClass:Class)-[:CONTAINS_FIELD]->(candidateField:Field {isfinal:false})<-[:AGGREGATED_FIELD_READ]-(method:Method)\n\u0009USING INDEX candidateField:Field(isfinal)\nMATCH (candidateField)-[:DATA_FLOW]->(condVariable:Assignment)-[:DATA_FLOW]->(condition:Condition {operation:\"!=\"})\n\u0009USING INDEX condition:Condition(operation)\n\u0009WHERE condition.operand1 = \"null\" OR condition.operand2 = \"null\"\nMATCH (condVariable)<-[:CONTROL_FLOW]-(ifStmt:NopStmt)  \n\u0009USING INDEX ifStmt:NopStmt(nopkind)\n\u0009WHERE ifStmt.nopkind = \"IF_COND\" OR (ifStmt) <-[:CONTROL_FLOW]- (:Condition)\nMATCH p=shortestPath((ifStmt)-[:CONTROL_FLOW*0..10]->(return:ReturnStmt))\n\u0009WHERE (return)-[:LAST_UNIT]->(method)\nRETURN DISTINCT candidateField, method, condition, condVariable, ifStmt, return";
	
	public static void main(String[] args) {
		JavaNeo4JTest test = new JavaNeo4JTest();
		test.transform();
	}
	
	
	public void transform(){
		
		long startTime = System.currentTimeMillis();
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
		
		/*
		 * Find candidates using the Cypher Match Query
		 */
		Result queryResult =  db.execute(JavaNeo4JTest.matchQuery);
		ResourceIterator<Node> candidateFields = queryResult.columnAs("candidateField");
		
		/*
		 * Collect distinct Candidate Fields
		 */
		Map<String, Node> distinctCandidateFields = new HashMap<>();
		while (candidateFields.hasNext()){
			Node node = candidateFields.next();
			if (!distinctCandidateFields.containsKey(node.getId())){
				distinctCandidateFields.put(String.valueOf(node.getId()), node);
			}
		}
		
//		for (Map.Entry<String, Node> distinctCandidate : distinctCandidateFields.entrySet()){
//			Node node = distinctCandidate.getValue();
//			
//			System.out.println(node.toString());
//			printNode(node);
//			System.out.println();
//		}
		
		/*
		 * Create new classes for each candidate
		 */
		for (Map.Entry<String, Node> distinctCandidate : distinctCandidateFields.entrySet()){
			
			String vartype = "";
			try (Transaction tx = db.beginTx()) {
				vartype = (String) distinctCandidate.getValue().getProperty("vartype");
				tx.success();
			}
			
			String classQuery = 
					"MATCH (class:Class) "
						+ "WHERE class.fqn = \"" + vartype + "\" "
					+ "RETURN class";
			Result classQueryResult = db.execute(classQuery);
			
			String packageQuery = 
					"MATCH (package:Package) "
							+ "WHERE package.name = \"" + StringUtil.extractPackagePath(vartype) + "\" "
					+ "RETURN package";
			Result packageQueryResult = db.execute(packageQuery);				
			
			ResourceIterator<Node> classes = classQueryResult.columnAs("class");
			ResourceIterator<Node> packages = packageQueryResult.columnAs("package");
			
			Node packageNode = packages.next();
			
			
			int numClasses = 0;
			while (classes.hasNext()){
				if (++numClasses > 1) {
					System.err.println("Too many classes to transform!");
				}
				Node classNode = classes.next();
				
				try (Transaction tx = db.beginTx()){
					
					Map<String,Object> properties = classNode.getAllProperties();
					Label classLabel = classNode.getLabels().iterator().next();
					
					Node realNode = db.createNode(classLabel);
					realNode.setProperty("fqn", StringUtil.addPrefixToClass("Real", properties.get("fqn")));
					realNode.setProperty("displayname", "Real"+properties.get("displayname"));
					realNode.setProperty("name", "Real"+properties.get("name"));
					realNode.setProperty("visibility", properties.get("visibility"));
					realNode.setProperty("origin", properties.get("origin"));
					realNode.setProperty("type", properties.get("type"));
					
					/*Relationship relationship = */
					realNode.createRelationshipTo(classNode, RelTypes.EXTENDS);
					packageNode.createRelationshipTo(realNode, RelTypes.CONTAINS_TYPE);
					
					
					Node nullNode = db.createNode(classLabel);
					nullNode.setProperty("fqn", StringUtil.addPrefixToClass("Null", properties.get("fqn")));
					nullNode.setProperty("displayname", "Null"+properties.get("displayname"));
					nullNode.setProperty("name", "Null"+properties.get("name"));
					nullNode.setProperty("visibility", properties.get("visibility"));
					nullNode.setProperty("origin", properties.get("origin"));
					nullNode.setProperty("type", properties.get("type"));
					
					nullNode.createRelationshipTo(classNode, RelTypes.EXTENDS);
					packageNode.createRelationshipTo(nullNode, RelTypes.CONTAINS_TYPE);
					
					tx.success();
				}
				System.out.println(classNode.toString());
			}
			
			
			
			
			
//			System.out.println(vartype);
//			System.out.println(classQueryResult.columns().get(0));
//			ResourceIterator<Node> classes = classQueryResult.columnAs("class");
//			while(classes.hasNext()){
//				Node node = classes.next();
//				System.out.println(node.toString());
//			}
			
//			System.out.println("end\n");
		}
		
		
		
		
		System.out.println("Time spent: " + (System.currentTimeMillis() - startTime) + " ms");
	}
	
	
	
	/**
	 * Prints the properties of a node to console.
	 * @param node
	 */
	public void printNode (Node node){
		try(Transaction tx = db.beginTx()){
			
			for (String property : node.getPropertyKeys()){
				System.out.printf("%s = %s%n", property, node.getProperty(property));
			}
			
			tx.success();
		}
	}
		
}
