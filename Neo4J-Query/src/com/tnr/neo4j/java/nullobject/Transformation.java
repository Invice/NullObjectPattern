package com.tnr.neo4j.java.nullobject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
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
	
	private final GraphDatabaseService dbService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(Constants.GraphDatabaseLocation + Constants.GraphDatabaseName));
	
	/*
	 * Used by method createIndexes().
	 */
	private Iterable<IndexDefinition> indexes;
	private Schema schema = null;
	
	/*
	 * Used by transforming and matching methods.
	 */
	private boolean hasMatched = false;
	Map<String, Node> distinctCandidateFields = new HashMap<>();
	
	private enum RelTypes implements RelationshipType {
		EXTENDS, CONTAINS_TYPE, CONTAINS_METHOD, DATA_FLOW, CONTROL_FLOW, CALLS, 
		AGGREGATED_CALLS, LAST_UNIT, CONTAINS_FIELD, CONTAINS_UNIT, CONTAINS_CONSTRUCTOR
	}
	
	
	/**
	 * Creates necessary indexes, if they don't already exist.
	 */
	public void createIndexes() {
		try (Transaction tx = dbService.beginTx()){
			
			if (schema == null){
				schema = dbService.schema();
			}
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
			}
			tx.success();
		}
	}
	
	/**
	 * Prints the existing indexes to console.
	 */
	public void getIndexes() {
		try (Transaction tx = dbService.beginTx()){
			if (schema == null){
				schema = dbService.schema();
			}
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
		Result queryResult =  dbService.execute(Constants.MATCH_QUERY);
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

		System.out.println("Transformed nodes:");
		for (Map.Entry<String, Node> distinctCandidate : distinctCandidateFields.entrySet()){
			
			String candidateVartype = "";
			try (Transaction tx = dbService.beginTx()) {
				candidateVartype = (String) distinctCandidate.getValue().getProperty("vartype");
				tx.success();
			}
			
			/*
			 * Find class node and package node of a candidate field
			 */
			String classQuery = 
					"MATCH (class:Class) "
						+ "USING INDEX class:Class(fqn)"
						+ "WHERE class.fqn = \"" + candidateVartype + "\" "
					+ "RETURN class";
			Result classQueryResult = dbService.execute(classQuery);
			
			String packageQuery = 
					"MATCH (package:Package) "
							+ "USING INDEX package:Package(name)"
							+ "WHERE package.name = \"" + StringUtil.extractPackagePath(candidateVartype) + "\" "
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
				
				// TODO: Check for availability of prefix+className
				String realPrefix = "Real";
				String abstractPrefix = "Abstract";
				String nullPrefix = "Null";
				
				try (Transaction tx = dbService.beginTx()){
					
					Map<String,Object> properties = classNode.getAllProperties();
					final long id = classNode.getId();
					Label classLabel = classNode.getLabels().iterator().next();
					
					/*
					 * Create new class paths for each node.
					 */
					final String realFqn = StringUtil.addPrefixToClass(realPrefix, properties.get("fqn"));
					final String nullFqn = StringUtil.addPrefixToClass(nullPrefix, properties.get("fqn"));
					final String abstractFqn = StringUtil.addPrefixToClass(abstractPrefix, properties.get("fqn"));
					
					/*
					 * Create realNode and relationships
					 */
					Node realNode = dbService.createNode(classLabel);
					realNode.setProperty("fqn", realFqn);
					realNode.setProperty("displayname", realPrefix+properties.get("displayname"));
					realNode.setProperty("name", realPrefix+properties.get("name"));
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
					nullNode.setProperty("displayname", nullPrefix+properties.get("displayname"));
					nullNode.setProperty("name", nullPrefix+properties.get("name"));
					nullNode.setProperty("visibility", properties.get("visibility"));
					nullNode.setProperty("origin", properties.get("origin"));
					nullNode.setProperty("type", properties.get("type"));
					
					nullNode.createRelationshipTo(classNode, RelTypes.EXTENDS);
					packageNode.createRelationshipTo(nullNode, RelTypes.CONTAINS_TYPE);
					
					
					/*
					 * Change node to abstractNode and update candidate vartype.
					 */
					classNode.setProperty("fqn", abstractFqn);
					classNode.setProperty("displayname", abstractPrefix + properties.get("displayname"));
					classNode.setProperty("name", abstractPrefix + properties.get("name"));
					distinctCandidate.getValue().setProperty("vartype", abstractFqn);
					
					/*
					 * Create new constructors for nullNode and realNode.
					 */
					// TODO: multiple constructors.
					// TODO: constructors & fields
					// TODO: Update abstract constructor
					// TODO: Insert nullNode into mainConstructor to initiate with NullObject
					Node nullConstructorNode = dbService.createNode(Label.label("Constructor"));
					Node realConstructorNode = dbService.createNode(Label.label("Constructor"));
					
					Iterable<Relationship> classOutRels = classNode.getRelationships(Direction.OUTGOING);
					for (Relationship rel : classOutRels){
						if (rel.isType(RelTypes.CONTAINS_CONSTRUCTOR)){
							Node constructorNode = rel.getEndNode();
							
							//Transfer old constructor calls to the new constructor for the real node.
							Iterable<Relationship> constructorInRels = constructorNode.getRelationships(Direction.INCOMING);
							for (Relationship constructorRel: constructorInRels){
								if (constructorRel.isType(RelTypes.AGGREGATED_CALLS)){
									constructorRel.getStartNode().createRelationshipTo(realConstructorNode, constructorRel.getType());
									constructorRel.delete();
								} else if (constructorRel.isType(RelTypes.CALLS)) {
									Node startNode = constructorRel.getStartNode();
									
									startNode.setProperty("fqn", realFqn + ".<init>()");
									
									Node newAssignNode = startNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.INCOMING).getStartNode();
									newAssignNode.setProperty("vartype", realFqn);
									String rightValue = "new " + realFqn;
									newAssignNode.setProperty("rightValue", rightValue);
									newAssignNode.setProperty("displayname", newAssignNode.getProperty("var") + " = " + rightValue);
									
									
									startNode.createRelationshipTo(realConstructorNode, constructorRel.getType());
									constructorRel.delete();
								}
							}
							createConstructorFlow(constructorNode, nullConstructorNode, nullFqn);
							nullNode.createRelationshipTo(nullConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
							
							createConstructorFlow(constructorNode, realConstructorNode, realFqn);
							realNode.createRelationshipTo(realConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
							break;
						}
					}
					
					/*
					 * Create new methods and change alignments.
					 */
					// TODO: Update method flow.
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
									// Delete old contains relationship.
									rel.delete();
								} else if (!rel.isType(RelTypes.CONTROL_FLOW) 
										&& !rel.isType(RelTypes.LAST_UNIT)){
									// Update relationships and caller fqn/rightvalue.
									Node startNode = rel.getStartNode();
									Node endNode = rel.getEndNode();
									long endNodeID = endNode.getId();
									
									if (rel.isType(RelTypes.CALLS) && endNodeID == methodId){
										String newFqn = StringUtil.addClassPathToMethod(abstractFqn, methodFqn);
										if (startNode.hasLabel(Label.label("MethodCallWithReturnValue"))){
											startNode.setProperty("fqn", newFqn);
											startNode.setProperty("rightValue", StringUtil.buildRightValue(abstractFqn, startNode.getAllProperties()));
										} else if (startNode.hasLabel(Label.label("MethodCall"))) {
											startNode.setProperty("fqn", newFqn);
										}
									}
									
									if (endNodeID == methodId){
										startNode.createRelationshipTo(abstractMethodNode, rel.getType());
										rel.delete();
									}
								}
							}
							classNode.createRelationshipTo(abstractMethodNode, RelTypes.CONTAINS_METHOD);
							nullNode.createRelationshipTo(nullMethodNode, RelTypes.CONTAINS_METHOD);
							
							/*
							 * Create control flow for null method.
							 */
							
							Node thisNode = dbService.createNode(Label.label("Assignment"));
							nullMethodNode.createRelationshipTo(thisNode, RelTypes.CONTROL_FLOW);
							nullMethodNode.createRelationshipTo(thisNode, RelTypes.CONTAINS_UNIT);
							
							thisNode.setProperty("operation", "thisdeclaration");
							thisNode.setProperty("type", "Assignment");
							thisNode.setProperty("var", "this");
							thisNode.setProperty("vartype", nullFqn);
							thisNode.setProperty("rightValue", "@this: " + nullFqn);
							thisNode.setProperty("displayname", "this = @this: " + nullFqn);
							
							Node returnNode = dbService.createNode(Label.label("ReturnStmt"));
							returnNode.setProperty("type", "ReturnStmt");
							returnNode.setProperty("displayname", "return");
							returnNode.createRelationshipTo(nullMethodNode, RelTypes.LAST_UNIT);
							
							//Create nodes for each parameter
							int parameterscount = (int) nullMethodNode.getProperty("parameterscount");
							Node lastNode = thisNode;
							for (int i = 0; i < parameterscount; i++) {
								Node parameterNode = dbService.createNode(Label.label("Assignment"));
								lastNode.createRelationshipTo(parameterNode, RelTypes.CONTROL_FLOW);
								String thisVartype = nullMethodNode.getProperty("p" + i).toString();
								
								parameterNode.setProperty("vartype", thisVartype);
								parameterNode.setProperty("var", "arg" + i);
								parameterNode.setProperty("rightValue", "@parameter" + i + ": " + thisVartype);
								parameterNode.setProperty("displayname", "arg" + i + " = @parameter" + i + ": " + thisVartype);
								parameterNode.setProperty("type", "Assignment");
								parameterNode.setProperty("operation", "parameterdeclaration");
								
								lastNode = parameterNode;
							}
							lastNode.createRelationshipTo(returnNode, RelTypes.CONTROL_FLOW);
							
						} 
						else if (methodProperties.get("visibility").toString().equals("private")) {
							//Private methods keep their relationships as they are only known to their own class.
							Iterable<Relationship> rels = methodNode.getRelationships();
							
							for (Relationship rel: rels){
								if (rel.isType(RelTypes.CONTAINS_METHOD)){
									rel.delete();
								} else if (rel.isType(RelTypes.CALLS) && rel.getEndNodeId() == methodId){
									Node startNode = rel.getStartNode();
									String newFqn = StringUtil.addClassPathToMethod(realFqn, methodFqn);
									if (startNode.hasLabel(Label.label("MethodCallWithReturnValue"))){
										startNode.setProperty("fqn", newFqn);
										startNode.setProperty("rightValue", StringUtil.buildRightValue(realFqn, startNode.getAllProperties()));
									} else if (startNode.hasLabel(Label.label("MethodCall"))) {
										startNode.setProperty("fqn", newFqn);
									}
								}
							}
						}
						methodNode.setProperty("fqn", StringUtil.addClassPathToMethod(realFqn, methodFqn));
						realNode.createRelationshipTo(methodNode, RelTypes.CONTAINS_METHOD);
						
					}
					methods.close();
					
					
					/*
					 * Search the now abstract class node for fields and give them to the real node.
					 */
					Iterable<Relationship> classRels = classNode.getRelationships();
					for (Relationship rel : classRels){
						if (rel.isType(RelTypes.CONTAINS_FIELD)){
							realNode.createRelationshipTo(rel.getEndNode(), RelTypes.CONTAINS_FIELD);
							rel.delete();
						}
					}
					
					/*
					 * Change vartype of Fields similar to candidate and their assignments to abstract class path.
					 */
					updateFieldAssigments(distinctCandidate.getValue(), abstractFqn);
					ResourceIterator<Node> similarFields = dbService.findNodes(Label.label("Field"), "vartype", candidateVartype);
					while (similarFields.hasNext()){
						Node similarField = similarFields.next();
						similarField.setProperty("vartype", abstractFqn);
						updateFieldAssigments(similarField, abstractFqn);
					}
					similarFields.close();
					
					tx.success();
				}
				System.out.println(classNode.toString());
//				printNode(classNode);
			}
			classes.close();
			
		}		
			
		System.out.println("Time spent transforming: " + (System.currentTimeMillis() - startTime) + " ms");
	}
	
	/**
	 * Updates the vartype of the incoming and outgoing assignments of a field.
	 * This method is to be used inside the transaction of the transform() method of this class.
	 * @param fieldNode
	 * @param vartype
	 */
	private void updateFieldAssigments(Node fieldNode, String vartype){
		String candidateName = fieldNode.getProperty("name").toString();
		Iterable<Relationship> candidateOutRels = fieldNode.getRelationships(Direction.OUTGOING);
		for (Relationship rel : candidateOutRels) {
			
			if (rel.isType(RelTypes.DATA_FLOW) && rel.getEndNode().hasLabel(Label.label("Assignment"))){
				Node endNode = rel.getEndNode();
				endNode.setProperty("vartype", vartype);
				String displayname = endNode.getProperty("displayname").toString();
				endNode.setProperty("displayname", StringUtil.buildOutgoingDisplayname(vartype, displayname, candidateName));
				
			}
		}
		Iterable<Relationship> candidateInRels = fieldNode.getRelationships(Direction.INCOMING);
		for (Relationship rel : candidateInRels) {
			
			if (rel.isType(RelTypes.DATA_FLOW) && rel.getStartNode().hasLabel(Label.label("Assignment"))){
				Node startNode = rel.getStartNode();
				startNode.setProperty("vartype", vartype);
				String displayname = startNode.getProperty("displayname").toString();
				startNode.setProperty("displayname", StringUtil.buildIncomingDisplayname(vartype, displayname, candidateName));
				
			}
		}
	}
	
	
	
	
	
	/**
	 * Creates the controlFlow for a new constructor using the super constructor.
	 * This method is to be used inside the transaction of the transform() method of this class.
	 * @param superConstructor the super constructor to call (default should be Object)
	 * @param newConstructor the new constructor
	 * @return
	 */
	private void createConstructorFlow (Node superConstructor, Node newConstructor, String classFqn){
		
			Map<String, Object> superConstructorProperties = superConstructor.getAllProperties();
			Node ctorNode = newConstructor;
			Node thisNode = dbService.createNode(Label.label("Assignment"));
			Node superNode = dbService.createNode(Label.label("ConstructorCall"));
			Node returnNode = dbService.createNode(Label.label("ReturnStmt"));
			
			ctorNode.createRelationshipTo(superConstructor, RelTypes.AGGREGATED_CALLS);
			ctorNode.createRelationshipTo(thisNode, RelTypes.CONTAINS_UNIT);
			ctorNode.createRelationshipTo(thisNode, RelTypes.CONTROL_FLOW);
			
			thisNode.createRelationshipTo(superNode, RelTypes.DATA_FLOW);
			thisNode.createRelationshipTo(superNode, RelTypes.CONTROL_FLOW);
			
			superNode.createRelationshipTo(superConstructor, RelTypes.CALLS);
			superNode.createRelationshipTo(returnNode, RelTypes.CONTROL_FLOW);
			
			returnNode.createRelationshipTo(ctorNode, RelTypes.LAST_UNIT);
			
			for(String key : superConstructorProperties.keySet()){
				if (!key.equals("fqn")){
					ctorNode.setProperty(key, superConstructorProperties.get(key));
				} else {
					ctorNode.setProperty("fqn", classFqn + ".<init>()");
				}
			}
			
			thisNode.setProperty("vartype", classFqn);
			thisNode.setProperty("var", "this");
			String rightValue = "@this: " + classFqn;
			thisNode.setProperty("displayname", "this = " + rightValue);
			thisNode.setProperty("rightValue", rightValue);
			thisNode.setProperty("lineNumber", 3);
			thisNode.setProperty("type", "Assignment");
			thisNode.setProperty("operation", "thisdeclaration");
			
			// TODO: Create option for constructor with arguments
			String superArgs = "";
			for (int i = 0; i < (int)superConstructorProperties.get("parameterscount"); i++){
				superArgs = superArgs + ", arg" + i;
			}
			
			
			superNode.setProperty("args", superArgs);
			superNode.setProperty("caller", "this");
			superNode.setProperty("fqn", superConstructorProperties.get("fqn"));
			superNode.setProperty("argumentscount", superConstructorProperties.get("parameterscount"));
			superNode.setProperty("displayname", "<init>()");
			superNode.setProperty("returntype", "void");
			superNode.setProperty("name", "super");
			superNode.setProperty("type", "ConstructorCall");
			
			returnNode.setProperty("displayname", "return");
			returnNode.setProperty("type", "ReturnStmt");
			
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
