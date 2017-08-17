package com.tnr.neo4j.java.nullobject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

import com.tnr.neo4j.java.nullobject.util.Constants;
import com.tnr.neo4j.java.nullobject.util.StringUtil;


public class Transformation {
	
	private GraphDatabaseService dbService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(Constants.GraphDatabaseLocation + Constants.GraphDatabaseName));
	
	/*
	 * Used by method createIndexes().
	 */
	private Iterable<IndexDefinition> indexes;
	private Schema schema;
	
	/*
	 * Used by transforming and matching methods.
	 */
	private boolean hasMatched = false;
	Map<String, Node> distinctCandidateFields = new HashMap<>();
	
	private enum RelTypes implements RelationshipType {
		EXTENDS, CONTAINS_TYPE, CONTAINS_METHOD, DATA_FLOW, CONTROL_FLOW, CALLS, AGGREGATED_CALLS, LAST_UNIT
	}
	
	public static final String matchQuery = ""
			+ "MATCH (mainClass:Class)-[:CONTAINS_FIELD]->(candidateField:Field {isfinal:false})<-[:AGGREGATED_FIELD_READ]-(method:Method)\n\u0009"
				+ "USING INDEX candidateField:Field(isfinal)\n"
			+ "MATCH (candidateField)-[:DATA_FLOW]->(condVariable:Assignment)-[:DATA_FLOW]->(condition:Condition {operation:\"!=\"})\n\u0009"
				+ "USING INDEX condition:Condition(operation)\n\u0009"
				+ "WHERE condition.operand1 = \"null\" "
					+ "OR condition.operand2 = \"null\"\n"
			+ "MATCH (condVariable)<-[:CONTROL_FLOW]-(ifStmt:NopStmt)  \n\u0009"
				+ "WHERE ifStmt.nopkind = \"IF_COND\" "
					+ "OR (ifStmt) <-[:CONTROL_FLOW]- (:Condition)\n"
			+ "MATCH p=shortestPath((ifStmt)-[:CONTROL_FLOW*0..]->(return:ReturnStmt))\n\u0009"
				+ "WHERE (return)-[:LAST_UNIT]->(method)\n"
			+ "RETURN DISTINCT candidateField";//, method, condition, condVariable, ifStmt, return";
	
	public static final String indexQuery = ""
			+ "CREATE INDEX ON :Condition(operation) "
			+ "CREATE INDEX ON :Field(isfinal) "
			+ "CREATE INDEX ON :NopStmt(nopkind) "
			+ "CREATE INDEX ON :Class(fqn) "
			+ "CREATE INDEX ON :Package(name) ";
			
	/**
	 * Creates necessary indexes, if they don't already exist.
	 */
	public void createIndexes() {
		try (Transaction tx = dbService.beginTx()){
			schema = dbService.schema();
			indexes = schema.getIndexes();
			tx.success();
		}
		
		createIndex("Field", "isfinal");
		createIndex("Condition", "operation");
		createIndex("NopStmt", "nopkind");
		createIndex("Class","fqn");
		createIndex("Package", "name");
	}
	
	/*
	 * Called by createIndexes().
	 */
	private void createIndex(String label, String property){
		
		boolean indexed = false;
		
		try(Transaction tx = dbService.beginTx()){
			
			for (IndexDefinition def : indexes){
				indexed = indexed || (def.getLabel().name().equals(label));
			}
			
			if (!indexed) {
					schema.indexFor(Label.label(label)).on(property).create();
					tx.success();
			}
		}
	}
	
	/**
	 * Prints the existing indexes to console.
	 */
	public void getIndexes() {
		try (Transaction tx = dbService.beginTx()){
			Schema schema = dbService.schema();
	
			Iterable<IndexDefinition> indexes = schema.getIndexes();
			
			for (IndexDefinition def : indexes) {
				System.out.print(":"+def.getLabel());
				for (String key : def.getPropertyKeys()){
					System.out.print("(" + key +")");
				}
				System.out.println();
			}
			tx.success();
		} 
	}
	
	/**
	 * Matches candidates for further processing.
	 */
	public void match() {
		/*
		 * Find candidates using the Cypher Match Query
		 */
		Result queryResult =  dbService.execute(Transformation.matchQuery);
		ResourceIterator<Node> candidateFields = queryResult.columnAs("candidateField");
		
		/*
		 * Collect distinct Candidate Fields
		 */
		distinctCandidateFields = new HashMap<>();
		while (candidateFields.hasNext()){
			Node node = candidateFields.next();
			if (!distinctCandidateFields.containsKey(node.getId())){
				distinctCandidateFields.put(String.valueOf(node.getId()), node);
			}
		}
		candidateFields.close();
		hasMatched = true;
	}
	
	
	public void transform(){
		
		long startTime = System.currentTimeMillis();
		
		if (!hasMatched) {
			System.out.println("Call match() before calling transform().");
			return;
		}
		
		
		/*
		 * Create new class nodes for each candidate
		 */
		for (Map.Entry<String, Node> distinctCandidate : distinctCandidateFields.entrySet()){
			
			String vartype = "";
			try (Transaction tx = dbService.beginTx()) {
				vartype = (String) distinctCandidate.getValue().getProperty("vartype");
				tx.success();
			}
			
			/*
			 * Find class node and package node of a candidate field
			 */
			String classQuery = 
					"MATCH (class:Class) "
						+ "USING INDEX class:Class(fqn)"
						+ "WHERE class.fqn = \"" + vartype + "\" "
					+ "RETURN class";
			Result classQueryResult = dbService.execute(classQuery);
			
			String packageQuery = 
					"MATCH (package:Package) "
							+ "USING INDEX package:Package(name)"
							+ "WHERE package.name = \"" + StringUtil.extractPackagePath(vartype) + "\" "
					+ "RETURN package";
			Result packageQueryResult = dbService.execute(packageQuery);
			
			
			ResourceIterator<Node> classes = classQueryResult.columnAs("class");
			ResourceIterator<Node> packages = packageQueryResult.columnAs("package");
			
			Node packageNode = packages.next();
			packages.close();
			
			int numClasses = 0;
			while (classes.hasNext()){
				if (++numClasses > 1) {
					System.err.println("Too many classes to transform!");
				}
				Node classNode = classes.next();
				
				try (Transaction tx = dbService.beginTx()){
					
					Map<String,Object> properties = classNode.getAllProperties();
					final long id = classNode.getId();
					Label classLabel = classNode.getLabels().iterator().next();
					
					/*
					 * Create new class paths for each node.
					 */
					final String realFqn = StringUtil.addPrefixToClass("Real", properties.get("fqn"));
					final String nullFqn = StringUtil.addPrefixToClass("Null", properties.get("fqn"));
					final String abstractFqn = StringUtil.addPrefixToClass("Abstract", properties.get("fqn"));
					
					/*
					 * Create realNode and relationships
					 */
					Node realNode = dbService.createNode(classLabel);
					realNode.setProperty("fqn", realFqn);
					realNode.setProperty("displayname", "Real"+properties.get("displayname"));
					realNode.setProperty("name", "Real"+properties.get("name"));
					realNode.setProperty("visibility", properties.get("visibility"));
					realNode.setProperty("origin", properties.get("origin"));
					realNode.setProperty("type", properties.get("type"));
					
					/*Relationship relationship = */
					realNode.createRelationshipTo(classNode, RelTypes.EXTENDS);
					packageNode.createRelationshipTo(realNode, RelTypes.CONTAINS_TYPE);
					
					/*
					 * Create nullNode and relationships
					 */
					Node nullNode = dbService.createNode(classLabel);
					nullNode.setProperty("fqn", nullFqn);
					nullNode.setProperty("displayname", "Null"+properties.get("displayname"));
					nullNode.setProperty("name", "Null"+properties.get("name"));
					nullNode.setProperty("visibility", properties.get("visibility"));
					nullNode.setProperty("origin", properties.get("origin"));
					nullNode.setProperty("type", properties.get("type"));
					
					nullNode.createRelationshipTo(classNode, RelTypes.EXTENDS);
					packageNode.createRelationshipTo(nullNode, RelTypes.CONTAINS_TYPE);
					
					
					/*
					 * Change node to abstractNode
					 */
					classNode.setProperty("fqn", abstractFqn);
					classNode.setProperty("displayname", "Abstract" + properties.get("displayname"));
					classNode.setProperty("name", "Abstract" + properties.get("name"));
					tx.success();
					
					/*
					 * Create new methods and change alignments.
					 */
					
					String methodQuery = 
							"MATCH (class:Class)-[:CONTAINS_METHOD]->(method:Method)\n"
									+ "WHERE id(class) = " + id +"\n"
									+ "RETURN method";
					Result methodQueryResult = dbService.execute(methodQuery);
					
					ResourceIterator<Node> methods = methodQueryResult.columnAs("method");
					
					while (methods.hasNext()){
						
						Node methodNode = methods.next();
						long methodId = methodNode.getId();
						Map <String, Object> methodProperties = methodNode.getAllProperties();
						String methodFqn = (String) methodProperties.get("fqn");
						
						if (methodProperties.get("visibility").toString().equals("public")) {
							
							/*
							 * Create new methods for null and abstract node.
							 */
							Node abstractMethodNode = dbService.createNode(Label.label("Method"));
							Node nullMethodNode = dbService.createNode(Label.label("Method"));
							
							for (String key : methodProperties.keySet()){
								if (!key.equals("fqn")){
									abstractMethodNode.setProperty(key, methodProperties.get(key));
									nullMethodNode.setProperty(key, methodProperties.get(key));
								}
							}
							abstractMethodNode.setProperty("isabstract", true);
							abstractMethodNode.setProperty("fqn", StringUtil.addClassPathToMethod(abstractFqn, methodFqn));
							nullMethodNode.setProperty("fqn", StringUtil.addClassPathToMethod(nullFqn, methodFqn));
							
							Iterable<Relationship> methodRels = methodNode.getRelationships();
							
							for (Relationship rel : methodRels){
								
								if (rel.isType(RelTypes.CONTAINS_METHOD)){
									rel.delete();
								} else if (!rel.isType(RelTypes.CONTROL_FLOW) 
										&& !rel.isType(RelTypes.LAST_UNIT)){
									Node startNode = rel.getStartNode();
									Node endNode = rel.getEndNode();
									
									if (endNode.getId() == methodId){
										
										startNode.createRelationshipTo(abstractMethodNode, rel.getType());
										rel.delete();
									}
								}
							}
							classNode.createRelationshipTo(abstractMethodNode, RelTypes.CONTAINS_METHOD);
							nullNode.createRelationshipTo(nullMethodNode, RelTypes.CONTAINS_METHOD);
						} 
						else if (methodProperties.get("visibility").toString().equals("private")) {
							Iterable<Relationship> rels = methodNode.getRelationships();
							
							for (Relationship rel: rels){
								if (rel.isType(RelTypes.CONTAINS_METHOD)){
									rel.delete();
								}
							}
						}
						
						methodNode.setProperty("fqn", StringUtil.addClassPathToMethod(realFqn, methodFqn));
						realNode.createRelationshipTo(methodNode, RelTypes.CONTAINS_METHOD);
						
						
					}
					methods.close();
					
					tx.close();
				}
				System.out.println(classNode.toString());
			}
			classes.close();
			
		}		
			
		System.out.println("Time spent transforming: " + (System.currentTimeMillis() - startTime) + " ms");
	}
	
	
	
	
	/**
	 * Prints the properties of a node to console.
	 * @param node
	 */
	public void printNode (Node node){
		try(Transaction tx = dbService.beginTx()){
			
			for (String property : node.getPropertyKeys()){
				System.out.printf("%s = %s%n", property, node.getProperty(property));
			}
			
			tx.success();
		}
	}
		
}