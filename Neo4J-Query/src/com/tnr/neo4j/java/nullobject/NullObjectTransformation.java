package com.tnr.neo4j.java.nullobject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

import com.tnr.neo4j.java.nullobject.matching.Matcher;
import com.tnr.neo4j.java.nullobject.util.Constants;
import com.tnr.neo4j.java.nullobject.util.RelTypes;
import com.tnr.neo4j.java.nullobject.util.SDGLabel;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyValues;
import com.tnr.neo4j.java.nullobject.util.StringUtil;


/**
 * Usage:
 * 
 *  1. createIndexes()
 *	2. match() 
 * 	3. transform()
 * 
 *	To check the indexes call getIndexes() after creating them.
 * 
 * @author Tim-Niklas Reck
 *
 */
public class NullObjectTransformation {
	
	/**
	 * The Neo4J DatabaseService that is managing our current database.
	 */
	private final GraphDatabaseService dbService;
	
	/**
	 * Used by method createIndexes().
	 */
	private Iterable<IndexDefinition> indexes;
	/**
	 * Used by method createIndexes().
	 */
	private Schema schema = null;
	
	/**
	 * Used by transforming and matching methods.
	 */
	private boolean hasMatchedCandidate = false;
	
	/**
	 * Used by transforming and matching methods.
	 */
	private Map<String, Node> distinctCandidateFields = new HashMap<>();
	private Map<String, Node> distinctConditionAssignments = new HashMap<>();
	private Map<String, Node> distinctCandidates = new HashMap<>();
	
	
	public NullObjectTransformation(String databasePath, String databaseName){
		dbService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(databasePath + databaseName));
	}
	
	public NullObjectTransformation(){
		dbService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(Constants.GraphDatabaseLocation + Constants.cacheDB));
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
		createIndex(SDGPropertyValues.TYPE_FIELD, SDGPropertyKey.ISFINAL);
		createIndex(SDGPropertyValues.TYPE_CONDITION, SDGPropertyKey.OPERATION);
		createIndex(SDGPropertyValues.TYPE_NOPSTMT, SDGPropertyKey.NOPKIND);
		createIndex(SDGPropertyValues.TYPE_CLASS, SDGPropertyKey.FQN);
		createIndex(SDGPropertyValues.TYPE_PACKAGE, SDGPropertyKey.NAME);
	}
	
	/**
	 * Called by createIndexes().
	 * @param label
	 * @param property
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
		
		Matcher matcher = new Matcher();
		
		hasMatchedCandidate = matcher.match(dbService);
		
		distinctCandidateFields = matcher.getDistinctCandidateFields();
		distinctConditionAssignments = matcher.getDistinctConditionAssignments();
		distinctCandidates = matcher.getDistinctCandidates();
	}
	
	/**
	 * Transforms the graphDb using candidates obtained with match().
	 */
	public void transform2nullObject(){
		
		long startTime = System.currentTimeMillis();
		
		/*
		 * This method must be called after matching.
		 */
		if (!hasMatchedCandidate) {
			System.out.println("Call match() before calling transform().");
			return;
		} else {
			System.out.println("Started transforming ...");
		}
		
		/*
		 * Transform each candidate individually.
		 */
		
		// TODO: Check uniqueness of class prefixes.
		for (Map.Entry<String, Node> candidate : distinctCandidates.entrySet()){
			
			
			Node candidateNode = candidate.getValue();
			Node packageNode = null;
			try (Transaction tx = dbService.beginTx()){
				packageNode = candidateNode.getSingleRelationship(RelTypes.CONTAINS_TYPE, Direction.INCOMING).getStartNode();
				tx.success();
			}
			
			
			/*
			 * Set default name prefixes for new classes.
			 */
			String realPrefix = "Real";
			String abstractPrefix = "Abstract";
			String nullPrefix = "Null";
			String candidateFqn = "";
			
			/*
			 * Create new real, null and abstract node for candidate.
			 */
			try (Transaction tx = dbService.beginTx()){
				
				Map<String,Object> candidateProperties = candidateNode.getAllProperties();
				final long candidateId = candidateNode.getId();
				candidateFqn = (String) candidateProperties.get(SDGPropertyKey.FQN);
				
				/*
				 * Create new class paths for each node.
				 */
				final String realFqn = StringUtil.addPrefixToClass(realPrefix, candidateProperties.get(SDGPropertyKey.FQN));
				final String nullFqn = StringUtil.addPrefixToClass(nullPrefix, candidateProperties.get(SDGPropertyKey.FQN));
				final String abstractFqn = StringUtil.addPrefixToClass(abstractPrefix, candidateProperties.get(SDGPropertyKey.FQN));
				
				/*
				 * Create realNode and relationships
				 */
				Node realNode = dbService.createNode(SDGLabel.CLASS);
				realNode.setProperty(SDGPropertyKey.FQN, realFqn);
				realNode.setProperty(SDGPropertyKey.DISPLAYNAME, realPrefix+candidateProperties.get(SDGPropertyKey.DISPLAYNAME));
				realNode.setProperty(SDGPropertyKey.NAME, realPrefix+candidateProperties.get(SDGPropertyKey.NAME));
				realNode.setProperty(SDGPropertyKey.VISIBILITY, candidateProperties.get(SDGPropertyKey.VISIBILITY));
				realNode.setProperty(SDGPropertyKey.ORIGIN, candidateProperties.get(SDGPropertyKey.ORIGIN));
				realNode.setProperty(SDGPropertyKey.TYPE, candidateProperties.get(SDGPropertyKey.TYPE));
				realNode.setProperty(SDGPropertyKey.ISABSTRACT, "false");
				realNode.setProperty(SDGPropertyKey.TRANSFORMED, "true");
				
				/*Relationship relationship = */
				realNode.createRelationshipTo(candidateNode, RelTypes.EXTENDS);
				packageNode.createRelationshipTo(realNode, RelTypes.CONTAINS_TYPE);
				
				/*
				 * Create nullNode and relationships
				 */
				Node nullNode = dbService.createNode(SDGLabel.CLASS);
				nullNode.setProperty(SDGPropertyKey.FQN, nullFqn);
				nullNode.setProperty(SDGPropertyKey.DISPLAYNAME, nullPrefix+candidateProperties.get(SDGPropertyKey.DISPLAYNAME));
				nullNode.setProperty(SDGPropertyKey.NAME, nullPrefix+candidateProperties.get(SDGPropertyKey.NAME));
				nullNode.setProperty(SDGPropertyKey.VISIBILITY, candidateProperties.get(SDGPropertyKey.VISIBILITY));
				nullNode.setProperty(SDGPropertyKey.ORIGIN, candidateProperties.get(SDGPropertyKey.ORIGIN));
				nullNode.setProperty(SDGPropertyKey.TYPE, candidateProperties.get(SDGPropertyKey.TYPE));
				nullNode.setProperty(SDGPropertyKey.ISABSTRACT, "false");
				nullNode.setProperty(SDGPropertyKey.TRANSFORMED, "true");
				
				nullNode.createRelationshipTo(candidateNode, RelTypes.EXTENDS);
				packageNode.createRelationshipTo(nullNode, RelTypes.CONTAINS_TYPE);
				
				
				/*
				 * Change node to abstractNode and update candidate vartype.
				 */
				candidateNode.setProperty(SDGPropertyKey.FQN, abstractFqn);
				candidateNode.setProperty(SDGPropertyKey.DISPLAYNAME, abstractPrefix + candidateProperties.get(SDGPropertyKey.DISPLAYNAME));
				candidateNode.setProperty(SDGPropertyKey.NAME, abstractPrefix + candidateProperties.get(SDGPropertyKey.NAME));
				candidateNode.setProperty(SDGPropertyKey.VARTYPE, abstractFqn);
				candidateNode.setProperty(SDGPropertyKey.ISABSTRACT, "true");
				candidateNode.setProperty(SDGPropertyKey.TRANSFORMED, "true");
				
				/*
				 * Create new constructors for nullNode and realNode.
				 */
				// TODO: multiple (overloaded) constructors.
			
				Node nullConstructorNode = dbService.createNode(SDGLabel.CONSTRUCTOR);
				Node realConstructorNode = dbService.createNode(SDGLabel.CONSTRUCTOR);
				
				Iterable<Relationship> classOutRels = candidateNode.getRelationships(RelTypes.CONTAINS_CONSTRUCTOR, Direction.OUTGOING);
				for (Relationship rel : classOutRels){
					Node constructorNode = rel.getEndNode();
					
					//Transfer old constructor calls to the new constructor for the real node.
					transferConstructorCalls(constructorNode, realConstructorNode, realFqn);
					
					//Transfer old constructor control flow to real constructor.
					transferConstructorFlow(constructorNode, realConstructorNode, realFqn);
					
					realNode.createRelationshipTo(realConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
					
					createConstructorFlow(constructorNode, nullConstructorNode, nullFqn);
					nullNode.createRelationshipTo(nullConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
					
					Relationship superCall = constructorNode.getSingleRelationship(RelTypes.AGGREGATED_CALLS, Direction.OUTGOING);
					Node superConstructor = superCall.getEndNode();
					superCall.delete();
					createConstructorFlow(superConstructor, constructorNode, abstractFqn);

					break;
				}
				
				/*
				 * Create new methods and change alignments.
				 */
				String methodQuery = 
						"MATCH (candidate:Class)-[:CONTAINS_METHOD]->(method:Method)\n"
								+ "WHERE id(candidate) = " + candidateId +"\n"
								+ "RETURN method";
				Result methodQueryResult = dbService.execute(methodQuery);
				
				ResourceIterator<Node> methods = methodQueryResult.columnAs("method");
				
				while (methods.hasNext()){
					
					Node methodNode = methods.next();
					long methodId = methodNode.getId();
					Map <String, Object> methodProperties = methodNode.getAllProperties();
					String methodFqn = (String) methodProperties.get(SDGPropertyKey.FQN);
					
					if (methodProperties.get(SDGPropertyKey.VISIBILITY).toString().equals(SDGPropertyValues.PUBLIC) 
							&& methodProperties.get(SDGPropertyKey.ISABSTRACT).toString().equals(SDGPropertyValues.FALSE)) {
						
						/*
						 * Create new methods for null and abstract node.
						 */
						Node abstractMethodNode = dbService.createNode(SDGLabel.METHOD);
						Node nullMethodNode = dbService.createNode(SDGLabel.METHOD);
						
						for (String key : methodProperties.keySet()){
							if (!key.equals(SDGPropertyKey.FQN)){
								abstractMethodNode.setProperty(key, methodProperties.get(key));
								nullMethodNode.setProperty(key, methodProperties.get(key));
							}
						}
						abstractMethodNode.setProperty(SDGPropertyKey.ISABSTRACT, true);
						abstractMethodNode.setProperty(SDGPropertyKey.FQN, StringUtil.addClassPathToMethod(abstractFqn, methodFqn));
						nullMethodNode.setProperty(SDGPropertyKey.FQN, StringUtil.addClassPathToMethod(nullFqn, methodFqn));
						
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
									if (startNode.hasLabel(SDGLabel.METHODCALLWITHRETURNVALUE)){
										startNode.setProperty(SDGPropertyKey.FQN, newFqn);
										startNode.setProperty(SDGPropertyKey.RIGHTVALUE, StringUtil.buildRightValue(abstractFqn, startNode.getAllProperties()));
									} else if (startNode.hasLabel(SDGLabel.METHODCALL)) {
										startNode.setProperty(SDGPropertyKey.FQN, newFqn);
									}
								}
								
								if (endNodeID == methodId){
									startNode.createRelationshipTo(abstractMethodNode, rel.getType());
									rel.delete();
								}
							}
						}
						candidateNode.createRelationshipTo(abstractMethodNode, RelTypes.CONTAINS_METHOD);
						nullNode.createRelationshipTo(nullMethodNode, RelTypes.CONTAINS_METHOD);
						
						/*
						 * Create control flow for null method.
						 */
						
						Node thisNode = dbService.createNode(SDGLabel.ASSIGNMENT);
						nullMethodNode.createRelationshipTo(thisNode, RelTypes.CONTROL_FLOW);
						nullMethodNode.createRelationshipTo(thisNode, RelTypes.CONTAINS_UNIT);
						
						thisNode.setProperty(SDGPropertyKey.OPERATION, "thisdeclaration");
						thisNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_ASSIGNMENT);
						thisNode.setProperty(SDGPropertyKey.VAR, "this");
						thisNode.setProperty(SDGPropertyKey.VARTYPE, nullFqn);
						thisNode.setProperty(SDGPropertyKey.RIGHTVALUE, "@this: " + nullFqn);
						thisNode.setProperty(SDGPropertyKey.DISPLAYNAME, "this = @this: " + nullFqn);
						
						
						//Create nodes for each parameter
						int parameterscount = (int) nullMethodNode.getProperty(SDGPropertyKey.PARAMETERSCOUNT);
						Node lastNode = thisNode;
						for (int i = 0; i < parameterscount; i++) {
							Node parameterNode = dbService.createNode(SDGLabel.ASSIGNMENT);
							lastNode.createRelationshipTo(parameterNode, RelTypes.CONTROL_FLOW);
							String thisVartype = nullMethodNode.getProperty("p" + i).toString();
							
							parameterNode.setProperty(SDGPropertyKey.VARTYPE, thisVartype);
							parameterNode.setProperty(SDGPropertyKey.VAR, "arg" + i);
							parameterNode.setProperty(SDGPropertyKey.RIGHTVALUE, "@parameter" + i + ": " + thisVartype);
							parameterNode.setProperty(SDGPropertyKey.DISPLAYNAME, "arg" + i + " = @parameter" + i + ": " + thisVartype);
							parameterNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_ASSIGNMENT);
							parameterNode.setProperty(SDGPropertyKey.OPERATION, "parameterdeclaration");
							
							lastNode = parameterNode;
						}
						
						//Create return node or return with argument.
						if (methodProperties.get(SDGPropertyKey.RETURNTYPE).toString().equals("void")){
							Node returnNode = dbService.createNode(SDGLabel.RETURNSTMT);
							returnNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_RETURNSTMT);
							returnNode.setProperty(SDGPropertyKey.DISPLAYNAME, "return");
							returnNode.createRelationshipTo(nullMethodNode, RelTypes.LAST_UNIT);
							lastNode.createRelationshipTo(returnNode, RelTypes.CONTROL_FLOW);
						} else {
							Node returnValueNode = dbService.createNode(SDGLabel.ASSIGNMENT);
							returnValueNode.setProperty(SDGPropertyKey.VARTYPE, methodProperties.get(SDGPropertyKey.RETURNTYPE));
							returnValueNode.setProperty(SDGPropertyKey.VAR, "temp$0");
							returnValueNode.setProperty(SDGPropertyKey.RIGHTVALUE, "null");
							returnValueNode.setProperty(SDGPropertyKey.DISPLAYNAME, "temp$0 = null");
							returnValueNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_ASSIGNMENT);
							returnValueNode.setProperty(SDGPropertyKey.OPERATION, "value");
							
							Node returnNode = dbService.createNode(SDGLabel.RETURNSTMT);
							returnNode.setProperty(SDGPropertyKey.DISPLAYNAME, "return temp$0");
							returnNode.setProperty(SDGPropertyKey.RIGHTVALUE, "temp$0");
							returnNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_RETURNSTMT);
							returnNode.setProperty(SDGPropertyKey.OPERATION, "value");
							
							lastNode.createRelationshipTo(returnValueNode, RelTypes.CONTROL_FLOW);
							returnValueNode.createRelationshipTo(returnNode, RelTypes.DATA_FLOW);
							returnValueNode.createRelationshipTo(returnNode, RelTypes.CONTROL_FLOW);
							returnNode.createRelationshipTo(nullMethodNode, RelTypes.LAST_UNIT);
						}
						
						
					} 
					else if (methodProperties.get(SDGPropertyKey.VISIBILITY).toString().equals(SDGPropertyValues.PRIVATE)) {
						//Private methods keep their relationships as they are only known to their own class.
						Iterable<Relationship> rels = methodNode.getRelationships();
						
						for (Relationship rel: rels){
							if (rel.isType(RelTypes.CONTAINS_METHOD)){
								rel.delete();
							} else if (rel.isType(RelTypes.CALLS) && rel.getEndNodeId() == methodId){
								Node startNode = rel.getStartNode();
								String newFqn = StringUtil.addClassPathToMethod(realFqn, methodFqn);
								if (startNode.hasLabel(SDGLabel.METHODCALLWITHRETURNVALUE)){
									startNode.setProperty(SDGPropertyKey.FQN, newFqn);
									startNode.setProperty(SDGPropertyKey.RIGHTVALUE, StringUtil.buildRightValue(realFqn, startNode.getAllProperties()));
								} else if (startNode.hasLabel(SDGLabel.METHODCALL)) {
									startNode.setProperty(SDGPropertyKey.FQN, newFqn);
								}
							}
						}
					}
					methodNode.setProperty(SDGPropertyKey.FQN, StringUtil.addClassPathToMethod(realFqn, methodFqn));
					Relationship thisRelation = methodNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING);
					if (thisRelation != null){
						updateThisAssignmentNode(thisRelation.getEndNode(), realFqn);
					}
					realNode.createRelationshipTo(methodNode, RelTypes.CONTAINS_METHOD);
				}
				methods.close();
				
				
				/*
				 * Search the now abstract class node for fields, update their assignments and give them to the real node.
				 */
				Iterable<Relationship> classFieldRels = candidateNode.getRelationships(RelTypes.CONTAINS_FIELD);
				for (Relationship rel : classFieldRels){
					Node fieldNode = rel.getEndNode();
					realNode.createRelationshipTo(fieldNode, RelTypes.CONTAINS_FIELD);
					updateInternalFieldAssignments(fieldNode, realFqn);
					rel.delete();
				}
				
				/*
				 * At this point the candidate has been transformed using the Null Object Pattern.
				 * Now we need to update field assigments and constructor calls.
				 */

				
				
			/*
			 * Transform candidate fields.
			 */
			for (Map.Entry<String, Node> distinctCandidateField : distinctCandidateFields.entrySet()){
				
				/*
				 * Get vartype of the candidate.
				 */
				String candidateFieldVartype = "";
				Node mainClass = null;
				
				candidateFieldVartype = (String) distinctCandidateField.getValue().getProperty(SDGPropertyKey.VARTYPE);
				mainClass = distinctCandidateField.getValue()
						.getSingleRelationship(RelTypes.CONTAINS_FIELD, Direction.INCOMING).getStartNode();
				
				/*
				 * Skip all candidate fields that do not belong to the current candidate.
				 */
				if (!candidateFieldVartype.equals(candidateFqn)){
					continue;
				}
				
				if (candidateFieldVartype.equals("")){
					System.err.println("Not a valid candidate field.");
					printNode(distinctCandidateField.getValue());
					return;
				} else if (mainClass == null) {
					System.err.println("No valid main class found.");
					printNode(distinctCandidateField.getValue());
					return;
				} else {
					System.out.println("Started transforming the field " + candidateFieldVartype + " contained in " 
							+ mainClass.getProperty(SDGPropertyKey.FQN) + ".");
				}
				
				/*
				 * Get assignments in if-statements belonging to this vartype.
				 */
				Map<String, Node> candidateConditionAssignments = new HashMap<>();
				for (String key : distinctConditionAssignments.keySet()){
					Node node = distinctConditionAssignments.get(key);
					if (node.getProperty(SDGPropertyKey.VARTYPE).toString().equals(candidateFieldVartype)){
						candidateConditionAssignments.put(key,node);
					}
				}
				System.out.println("Found [" + candidateConditionAssignments.size() + "] condition nodes belonging to fields of this candidate in this class.");
				
				/*
				 * Remove the assignments from if statements.
				 */
				for (String key : candidateConditionAssignments.keySet()){
					removeAssignmentFromConditions(candidateConditionAssignments.get(key));
					distinctConditionAssignments.remove(key);
				}
			}
			
			/*
			 * Change vartype of all fields with the previous type of the candidate node to the abstract type.
			 */			
		
			ResourceIterator<Node> similarFields = dbService.findNodes(SDGLabel.FIELD, SDGPropertyKey.VARTYPE, candidateFqn);
			List<Long> mainClassIds = new ArrayList<>();
			
			while (similarFields.hasNext()){
				Node similarField = similarFields.next();
				similarField.setProperty(SDGPropertyKey.VARTYPE, abstractFqn);
				updateFieldAssigments(similarField, abstractFqn);
				
				/*
				 * Add all mainClasses containing a candidate field to mainClass ids.
				 */
				Node mainClass = similarField.getSingleRelationship(RelTypes.CONTAINS_FIELD, Direction.INCOMING).getStartNode();
				long mainClassId = mainClass.getId();
				if (!mainClassIds.contains(mainClassId)){
					mainClassIds.add(mainClassId);
				}
			}
			similarFields.close();
			
			/*
			 * Find uninitialized fields of type abstractFqn for every class in mainClassIds an initialize them.
			 */
			for (long id : mainClassIds){
				Node mainClass = dbService.getNodeById(id);
				findAndInitializeUninitializedFields(mainClass, id, abstractFqn, nullNode);
			}
			
			//End of candidate Transaction
			tx.success();
		}
			
			System.out.println("Transformed candidate node: " + candidateFqn + " (" + candidateNode.toString() + ")");
		}	
		System.out.println("Finished transforming.");
		System.out.println("Time spent transforming: " + (System.currentTimeMillis() - startTime) + "ms");
		
	}
	
	/**
	 * Finds all fields of a vartype that are not initialized within the main class constructors and
	 * initializes them with the null object.
	 * @param mainClass
	 * @param mainClassId
	 * @param vartype
	 * @param nullNode
	 */
	private void findAndInitializeUninitializedFields(Node mainClass, long mainClassId, String vartype, Node nullNode){
		
		String uninitializedFieldQuery = ""
				+ "MATCH (n:Field {vartype:'" + vartype + "'})<-[:CONTAINS_FIELD]-(main:Class)"
				+ "WHERE id(main) = " + mainClassId 
					+ " AND NOT (n)<-[:AGGREGATED_FIELD_WRITE]-(:Constructor)<-[:CONTAINS_CONSTRUCTOR]-(main)"
				+ "RETURN n";
				
		Result result = dbService.execute(uninitializedFieldQuery);
		ResourceIterator<Node> fields = result.columnAs("n");
		List<Node> constructors = new ArrayList<Node>();
		for (Relationship rel : mainClass.getRelationships(RelTypes.CONTAINS_CONSTRUCTOR, Direction.OUTGOING)){
			constructors.add(rel.getEndNode());
		}
		
		String mainFqn = (String) mainClass.getProperty(SDGPropertyKey.FQN);
		int fieldNum = 0;
		while (fields.hasNext()){
			Node field = fields.next();
			for (Node constructor : constructors){
				injectFieldInitToConstructor(constructor, field, nullNode, fieldNum++, mainFqn);
			}
		}
		fields.close();
	}
	
	/**
	 * Adds the initialization sequence of a field to the constructor of a main class.
	 * @param constructor
	 * @param field
	 * @param nullNode
	 * @param num
	 * @param mainFqn
	 */
	private void injectFieldInitToConstructor(Node constructor, Node field, Node nullNode, int num, String mainFqn) {
		
		Node nullConstructor = nullNode.getSingleRelationship(RelTypes.CONTAINS_CONSTRUCTOR, Direction.OUTGOING).getEndNode();
		Node constructorThisNode = constructor.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
		Node constructorSuperNode = constructorThisNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
		Relationship superRel = constructorSuperNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING);
		Node nextNode = superRel.getEndNode();
		superRel.delete();
		
		
		Node tempAssign = dbService.createNode(SDGLabel.ASSIGNMENT);
		Node init = dbService.createNode(SDGLabel.CONSTRUCTOR);
		Node fieldAssign = dbService.createNode(SDGLabel.ASSIGNMENT);
		
		constructor.createRelationshipTo(field, RelTypes.AGGREGATED_FIELD_WRITE);
		constructorThisNode.createRelationshipTo(fieldAssign, RelTypes.DATA_FLOW);
		constructorSuperNode.createRelationshipTo(tempAssign, RelTypes.CONTROL_FLOW);
		tempAssign.createRelationshipTo(init, RelTypes.CONTROL_FLOW);
		tempAssign.createRelationshipTo(init, RelTypes.DATA_FLOW);
		tempAssign.createRelationshipTo(fieldAssign, RelTypes.DATA_FLOW);
		init.createRelationshipTo(nullConstructor, RelTypes.CALLS);
		init.createRelationshipTo(fieldAssign, RelTypes.CONTROL_FLOW);
		fieldAssign.createRelationshipTo(field, RelTypes.DATA_FLOW);
		fieldAssign.createRelationshipTo(nextNode, RelTypes.CONTROL_FLOW);
		
		String nullFqn = (String) nullNode.getProperty(SDGPropertyKey.FQN);
		String var = "newTemp$" + num;
		String rightValue = "new " + nullFqn;
		
		tempAssign.setProperty(SDGPropertyKey.VARTYPE, nullFqn);
		tempAssign.setProperty(SDGPropertyKey.VAR, var);
		tempAssign.setProperty(SDGPropertyKey.RIGHTVALUE, rightValue);
		tempAssign.setProperty(SDGPropertyKey.DISPLAYNAME , var + " = " + rightValue);
		tempAssign.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_ASSIGNMENT);
		tempAssign.setProperty(SDGPropertyKey.OPERATION, "new");
		
		init.setProperty(SDGPropertyKey.ARGS, new String[0]);
		init.setProperty(SDGPropertyKey.CALLER, var);
		init.setProperty(SDGPropertyKey.FQN, nullFqn + ".<init>()");
		init.setProperty(SDGPropertyKey.ARGUMENTSCOUNT, 0);
		init.setProperty(SDGPropertyKey.DISPLAYNAME, "<init>()");
		init.setProperty(SDGPropertyKey.RETURNTYPE, "void");
		init.setProperty(SDGPropertyKey.NAME, "<init>");
		init.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_CONSTRUCTORCALL);
		
		String fieldFqn = (String) field.getProperty(SDGPropertyKey.VARTYPE);
		String fieldName = (String) field.getProperty(SDGPropertyKey.NAME);
		fieldAssign.setProperty(SDGPropertyKey.VARTYPE, fieldFqn);
		fieldAssign.setProperty(SDGPropertyKey.VAR, fieldName);
		fieldAssign.setProperty(SDGPropertyKey.RIGHTVALUE, var);
		fieldAssign.setProperty(SDGPropertyKey.DISPLAYNAME , "this.<" + mainFqn + ": " + fieldFqn + " " + fieldName + "> = " + var);
		fieldAssign.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_ASSIGNMENT);
		fieldAssign.setProperty(SDGPropertyKey.OPERATION, "value");
	}
	
	/**
	 * Updates the vartype of the incoming and outgoing assignments of a field in the mainclass.
	 * This method is to be used inside the transaction of the transform() method of this class.
	 * @param fieldNode
	 * @param vartype
	 */
	private void updateFieldAssigments(Node fieldNode, String vartype){
		String candidateName = fieldNode.getProperty(SDGPropertyKey.NAME).toString();
		Iterable<Relationship> candidateOutRels = fieldNode.getRelationships(RelTypes.DATA_FLOW, Direction.OUTGOING);
		for (Relationship rel : candidateOutRels) {
			
			if (rel.getEndNode().hasLabel(SDGLabel.ASSIGNMENT)){
				Node endNode = rel.getEndNode();
				endNode.setProperty(SDGPropertyKey.VARTYPE, vartype);
				String displayname = endNode.getProperty(SDGPropertyKey.DISPLAYNAME).toString();
				endNode.setProperty(SDGPropertyKey.DISPLAYNAME, StringUtil.buildOutgoingDisplayname(vartype, displayname, candidateName));
			}
		}
		Iterable<Relationship> candidateInRels = fieldNode.getRelationships(RelTypes.DATA_FLOW, Direction.INCOMING);
		for (Relationship rel : candidateInRels) {
			
			if (rel.getStartNode().hasLabel(SDGLabel.ASSIGNMENT)){
				Node startNode = rel.getStartNode();
				startNode.setProperty(SDGPropertyKey.VARTYPE, vartype);
				String displayname = startNode.getProperty(SDGPropertyKey.DISPLAYNAME).toString();
				startNode.setProperty(SDGPropertyKey.DISPLAYNAME, StringUtil.buildIncomingDisplayname(vartype, displayname, candidateName));
			}
		}
	}
	
	/**
	 * Updates the vartype of the incoming and outgoing assignments of a field belonging to the real node.
	 * This method is to be used inside the transaction of the transform() method of this class.
	 * @param fieldNode
	 * @param vartype
	 */
	private void updateInternalFieldAssignments(Node fieldNode, String vartype){
		Iterable<Relationship> fieldInRels = fieldNode.getRelationships(RelTypes.DATA_FLOW, Direction.INCOMING);
		for (Relationship rel : fieldInRels) {
			Node startNode = rel.getStartNode();
			String displayname = startNode.getProperty(SDGPropertyKey.DISPLAYNAME).toString();
			startNode.setProperty(SDGPropertyKey.DISPLAYNAME, StringUtil.buildIncomingInternalDisplayname(vartype, displayname));
		}
		
		Iterable<Relationship> fieldOutRels = fieldNode.getRelationships(RelTypes.DATA_FLOW, Direction.OUTGOING);
		for (Relationship rel : fieldOutRels) {
			Node endNode = rel.getEndNode();
			String displayname = endNode.getProperty(SDGPropertyKey.DISPLAYNAME).toString();
			endNode.setProperty(SDGPropertyKey.DISPLAYNAME, StringUtil.buildOutgoingInternalDisplayname(vartype, displayname));
		}
	}
	
	
	/**
	 * Updates the vartype, rightValue and displayname of a thisdeclaration node.
	 * @param thisNode
	 * @param vartype the vartype used to update.
	 */
	private void updateThisAssignmentNode(Node thisNode, String vartype){
		
		if (thisNode == null) {
			return;
		}
		
		String rightValue = "@this: " + vartype;
		thisNode.setProperty(SDGPropertyKey.VARTYPE, vartype);
		thisNode.setProperty(SDGPropertyKey.RIGHTVALUE, rightValue);
		thisNode.setProperty(SDGPropertyKey.DISPLAYNAME, "this = " + rightValue);
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
			Node thisNode = dbService.createNode(SDGLabel.ASSIGNMENT);
			Node superNode = dbService.createNode(SDGLabel.CONSTRUCTORCALL);
			Node returnNode = dbService.createNode(SDGLabel.RETURNSTMT);
			
			ctorNode.createRelationshipTo(superConstructor, RelTypes.AGGREGATED_CALLS);
			ctorNode.createRelationshipTo(thisNode, RelTypes.CONTAINS_UNIT);
			ctorNode.createRelationshipTo(thisNode, RelTypes.CONTROL_FLOW);
			
			thisNode.createRelationshipTo(superNode, RelTypes.DATA_FLOW);
			thisNode.createRelationshipTo(superNode, RelTypes.CONTROL_FLOW);
			
			superNode.createRelationshipTo(superConstructor, RelTypes.CALLS);
			superNode.createRelationshipTo(returnNode, RelTypes.CONTROL_FLOW);
			
			returnNode.createRelationshipTo(ctorNode, RelTypes.LAST_UNIT);
			
			for(String key : superConstructorProperties.keySet()){
				if (!key.equals(SDGPropertyKey.FQN)){
					ctorNode.setProperty(key, superConstructorProperties.get(key));
				} else {
					ctorNode.setProperty(SDGPropertyKey.FQN, classFqn + ".<init>()");
				}
			}
			
			thisNode.setProperty(SDGPropertyKey.VARTYPE, classFqn);
			thisNode.setProperty(SDGPropertyKey.VAR, "this");
			String rightValue = "@this: " + classFqn;
			thisNode.setProperty(SDGPropertyKey.DISPLAYNAME, "this = " + rightValue);
			thisNode.setProperty(SDGPropertyKey.RIGHTVALUE, rightValue);
			thisNode.setProperty(SDGPropertyKey.LINENUMBER, 3);
			thisNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_ASSIGNMENT);
			thisNode.setProperty(SDGPropertyKey.OPERATION, "thisdeclaration");
			
			// TODO: Create option for constructor with arguments
			
			int parameterscount = (int)superConstructorProperties.get(SDGPropertyKey.PARAMETERSCOUNT);
			String[] superArgs = new String[parameterscount];
			for (int i = 0; i < parameterscount; i++){
				superArgs[i] = "arg" + i;
			}
			
			
			superNode.setProperty(SDGPropertyKey.ARGS, superArgs);
			superNode.setProperty(SDGPropertyKey.CALLER, "this");
			superNode.setProperty(SDGPropertyKey.FQN, superConstructorProperties.get(SDGPropertyKey.FQN));
			superNode.setProperty(SDGPropertyKey.ARGUMENTSCOUNT, superConstructorProperties.get(SDGPropertyKey.PARAMETERSCOUNT));
			superNode.setProperty(SDGPropertyKey.DISPLAYNAME, "<init>()");
			superNode.setProperty(SDGPropertyKey.RETURNTYPE, "void");
			superNode.setProperty(SDGPropertyKey.NAME, "super");
			superNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_CONSTRUCTORCALL);
			
			returnNode.setProperty(SDGPropertyKey.DISPLAYNAME, "return");
			returnNode.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_RETURNSTMT);
	}
	
	/**
	 * Transfers the calls and aggregated calls of a constructor to another constructor while updating the contained nodes.
	 * This method is to be used inside the transaction of the transform() method of this class.
	 * @param oldConstructor
	 * @param newConstrutctor
	 * @param newFqn the fqn of the new constructor.
	 */
	private void transferConstructorCalls (Node oldConstructor, Node newConstrutctor, String newFqn){
		
		Iterable<Relationship> constructorInRels = oldConstructor.getRelationships(Direction.INCOMING);
		for (Relationship constructorRel: constructorInRels){
			if (constructorRel.isType(RelTypes.AGGREGATED_CALLS)){
				constructorRel.getStartNode().createRelationshipTo(newConstrutctor, constructorRel.getType());
				constructorRel.delete();
			} else if (constructorRel.isType(RelTypes.CALLS)) {
				Node startNode = constructorRel.getStartNode();
				
				startNode.setProperty(SDGPropertyKey.FQN, newFqn + ".<init>()");
				
				Node newAssignNode = startNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.INCOMING).getStartNode();
				
				newAssignNode.setProperty(SDGPropertyKey.VARTYPE, newFqn);
				String rightValue = "new " + newFqn;
				newAssignNode.setProperty(SDGPropertyKey.RIGHTVALUE, rightValue);
				newAssignNode.setProperty(SDGPropertyKey.DISPLAYNAME, newAssignNode.getProperty(SDGPropertyKey.VAR) + " = " + rightValue);
				
				startNode.createRelationshipTo(newConstrutctor, constructorRel.getType());
				constructorRel.delete();
			}
		}
	}
	
	/**
	 * Transfers the controlflow of a constructor to another constructor while updating the contained nodes.
	 * This method is to be used inside the transaction of the transform() method of this class.
	 * @param oldConstructor
	 * @param newConstructor
	 * @param newFqn the fqn of the new constructor.
	 */
	private void transferConstructorFlow(Node oldConstructor, Node newConstructor, String newFqn){
		
		if (oldConstructor.getProperty(SDGPropertyKey.ISABSTRACT).toString().equals(SDGPropertyValues.TRUE)){
			return;
		}
		
		Map<String, Object> oldConstructorProperties = oldConstructor.getAllProperties();
		
		for(String key : oldConstructorProperties.keySet()){
			if (!key.equals(SDGPropertyKey.FQN)){
				newConstructor.setProperty(key, oldConstructorProperties.get(key));
			} else {
				newConstructor.setProperty(SDGPropertyKey.FQN, newFqn + ".<init>()");
			}
		}
			
		Relationship lastUnit = oldConstructor.getSingleRelationship(RelTypes.LAST_UNIT, Direction.INCOMING);
		
		if (lastUnit != null) {
			lastUnit.getStartNode().createRelationshipTo(newConstructor, RelTypes.LAST_UNIT);
			lastUnit.delete();
		}
		
		
		
		Relationship controlFlow = oldConstructor.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING);
		Relationship containsUnit = oldConstructor.getSingleRelationship(RelTypes.CONTAINS_UNIT, Direction.OUTGOING);
				
		Node thisNode = controlFlow.getEndNode();
		
		newConstructor.createRelationshipTo(thisNode, RelTypes.CONTROL_FLOW);
		newConstructor.createRelationshipTo(thisNode, RelTypes.CONTAINS_UNIT);
		updateThisAssignmentNode(thisNode, newFqn);
		
		
		controlFlow.delete();
		containsUnit.delete();
		
		Node superNode = thisNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
		superNode.setProperty(SDGPropertyKey.FQN, oldConstructor.getProperty(SDGPropertyKey.FQN));
		superNode.setProperty(SDGPropertyKey.ARGUMENTSCOUNT, oldConstructor.getProperty(SDGPropertyKey.PARAMETERSCOUNT));
		superNode.getSingleRelationship(RelTypes.CALLS, Direction.OUTGOING).delete();
		superNode.createRelationshipTo(oldConstructor, RelTypes.CALLS);
		

		
		
		Iterable<Relationship> fieldWrites = oldConstructor.getRelationships(RelTypes.AGGREGATED_FIELD_WRITE);
		for (Relationship fieldWrite : fieldWrites){
			newConstructor.createRelationshipTo(fieldWrite.getEndNode(), RelTypes.AGGREGATED_FIELD_WRITE);
			fieldWrite.delete();
		}
		
		newConstructor.createRelationshipTo(oldConstructor, RelTypes.AGGREGATED_CALLS);
	}
	
	
	/**
	 * Removes the assignments and condition control flows of candidate assignments inside if statemens.
	 * @param assignNode
	 */
	private void removeAssignmentFromConditions(Node assignNode){
		// TODO : Update for IF_COND_X
		/*
		 * ifCondNopStmt should be of type (NopStmt {nopkind:IF_COND}) or belong to a if condition
		 * as the match query only collects those assign nodes in distinctConditionAssignments.
		 * Same for conditionNode.
		 */
		Node incomingNopStmt = assignNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.INCOMING).getStartNode();
		Node conditionNode = assignNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
		
		Node endCondNopStmt = null;
		long endCondId = 0;
		if (incomingNopStmt.hasProperty(SDGPropertyKey.NOPKIND)){
			endCondNopStmt = incomingNopStmt.getSingleRelationship(RelTypes.LAST_UNIT, Direction.INCOMING).getStartNode();
			endCondId = endCondNopStmt.getId();
		}
		
		Iterable<Relationship> conditionControlFlow = conditionNode.getRelationships(RelTypes.CONTROL_FLOW, Direction.OUTGOING);
		List<Node> followingNodes = new ArrayList<>();
		for(Relationship rel : conditionControlFlow){
			followingNodes.add(rel.getEndNode());
		}

		/*
		 * Test if the condition is the first and/or last condition of the if condition(s).
		 * If this is not the only condition, it can be removed from the conditions but the others must stay.
		 */
		
		boolean firstCondition = incomingNopStmt.hasProperty(SDGPropertyKey.NOPKIND)
				&& incomingNopStmt.getProperty(SDGPropertyKey.NOPKIND).toString().equals("IF_COND");
		
		
		boolean lastCondition = followingNodes.size()==2 
				&& followingNodes.get(0).hasProperty(SDGPropertyKey.NOPKIND) && followingNodes.get(1).hasProperty(SDGPropertyKey.NOPKIND);
		
		
		if (firstCondition && lastCondition) {
			// -> only condition
			Node callingNode = incomingNopStmt.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.INCOMING).getStartNode();
			Node thenNode = null;
			Node elseNode = null;
			
			for (Node nopNode: followingNodes) {
				String nopkind = nopNode.getProperty(SDGPropertyKey.NOPKIND).toString();
				if (nopkind.equals("IF_THEN")){
					thenNode = nopNode;
				} else if (nopkind.equals("IF_ELSE")) {
					elseNode = nopNode;
				} else {
					System.err.println("Wrong node inside control flow of an if statement.");
					return;
				}
			}
			
			/*
			 * Delete the else branch.
			 */
			boolean hasNextElseNode = true;
			while (hasNextElseNode) {
				Node nextNode = elseNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
				removeNodeFromDb(elseNode);
				elseNode = nextNode;
				hasNextElseNode = !(nextNode.getId() == endCondId);
			}
			
			/*
			 * Remove the endNode.
			 */
			Node lastThenNode = endCondNopStmt.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.INCOMING).getStartNode();
			Node calledNode = endCondNopStmt.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
			lastThenNode.createRelationshipTo(calledNode, RelTypes.CONTROL_FLOW);
			removeNodeFromDb(endCondNopStmt);
			
			/*
			 * Remove the thenNode, assignmentNode and condNode.
			 */
			Node firstThenNode = thenNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
			callingNode.createRelationshipTo(firstThenNode, RelTypes.CONTROL_FLOW);
			
			removeNodeFromDb(thenNode);
			removeNodeFromDb(incomingNopStmt);
			removeNodeFromDb(conditionNode);
			removeNodeFromDb(assignNode);
			
		} else if (firstCondition && !lastCondition) {
			Node unnamedNode = null;
			Node nextConditionNode = null;
			
			for (Node nopNode : followingNodes){
				if (!nopNode.hasProperty(SDGPropertyKey.NOPKIND)){
					unnamedNode = nopNode;
					nextConditionNode = nopNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.OUTGOING).getEndNode();
				}
			}
			incomingNopStmt.createRelationshipTo(nextConditionNode, RelTypes.CONTROL_FLOW);
			removeNodeFromDb(assignNode);
			removeNodeFromDb(conditionNode);
			removeNodeFromDb(unnamedNode);
			
		} else {
			
			Node unnamedNode = assignNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.INCOMING).getStartNode();
			Node previousCondition = unnamedNode.getSingleRelationship(RelTypes.CONTROL_FLOW, Direction.INCOMING).getStartNode();
			Node followingNopNode = null;
			
			for (Node nopNode : followingNodes) {
				if (!nopNode.hasProperty(SDGPropertyKey.NOPKIND) || nopNode.getProperty(SDGPropertyKey.NOPKIND).toString().equals("IF_THEN")){
					followingNopNode = nopNode;
				}
			}
			previousCondition.createRelationshipTo(followingNopNode, RelTypes.CONTROL_FLOW);
			
			removeNodeFromDb(unnamedNode);
			removeNodeFromDb(conditionNode);
			removeNodeFromDb(assignNode);
		}	
	}
	
	/**
	 * Removes a node and its relationships from the database. 
	 * To be used inside a transaction.
	 * @param node
	 */
	private void removeNodeFromDb (Node node) {
		for (Relationship rel : node.getRelationships()){
			rel.delete();
		}
		node.delete();
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
