package com.tnr.neo4j.java.nullobject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.tnr.neo4j.java.nullobject.index.Indexer;
import com.tnr.neo4j.java.nullobject.matching.Matcher;
import com.tnr.neo4j.java.nullobject.transformation.MethodTransformer;
import com.tnr.neo4j.java.nullobject.transformation.NodeCreator;
import com.tnr.neo4j.java.nullobject.transformation.util.PropertyContainer;
import com.tnr.neo4j.java.nullobject.util.Constants;
import com.tnr.neo4j.java.nullobject.util.RelTypes;
import com.tnr.neo4j.java.nullobject.util.SDGLabel;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyValues;
import com.tnr.neo4j.java.nullobject.util.StringUtil;
import com.tnr.neo4j.java.nullobject.util.query.QueryPaths;
import com.tnr.neo4j.java.nullobject.util.query.QueryReader;


/**
 * Usage:
 * 
 *  1. createIndexes()
 *	2. match() 
 * 	3. transform()
 * 
 *	To check the indexes call printIndexes() after creating them.
 * 
 * @author Tim-Niklas Reck
 *
 */
public class NullObjectTransformation implements Transformation {
	
	/**
	 * The Neo4J DatabaseService that is managing our current database.
	 */
	private final GraphDatabaseService dbService;
	
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
		Indexer index = new Indexer(dbService);
		
		index.createUniqueIndex(SDGPropertyValues.TYPE_FIELD, SDGPropertyKey.ISFINAL);
		index.createUniqueIndex(SDGPropertyValues.TYPE_CONDITION, SDGPropertyKey.OPERATION);
		index.createUniqueIndex(SDGPropertyValues.TYPE_NOPSTMT, SDGPropertyKey.NOPKIND);
		index.createUniqueIndex(SDGPropertyValues.TYPE_CLASS, SDGPropertyKey.FQN);
		index.createUniqueIndex(SDGPropertyValues.TYPE_PACKAGE, SDGPropertyKey.NAME);
	}
			
	/**
	 * Prints the existing indexes to console.
	 */
	public void printIndexes() {
		Indexer index = new Indexer(dbService);
		index.printIndexes();
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
	public void transform(){
		
		long startTime = System.currentTimeMillis();
		
		// This method must be called after matching.
		checkIsMatched();
		
		// Transform each candidate individually.
		// TODO: Check uniqueness of class prefixes.
		for (Map.Entry<String, Node> candidate : distinctCandidates.entrySet()){
			
			String candidateFqn = "";
			Node candidateNode = candidate.getValue();
			Node packageNode = null;
			
			try (Transaction tx = dbService.beginTx()){
				
				packageNode = candidateNode.getSingleRelationship(
						RelTypes.CONTAINS_TYPE, 
						Direction.INCOMING)
						.getStartNode();
				
				PropertyContainer propertyContainer = new PropertyContainer(candidateNode.getAllProperties());
				
				NodeCreator nodeCreator = new NodeCreator(dbService, propertyContainer);
				
				// Create realNode and nullNode with relationships.
				Node realNode = nodeCreator.createRealNode(packageNode, candidateNode);
				Node nullNode = nodeCreator.createNullNode(packageNode, candidateNode);
				
				 // Change candidate node to abstractNode and update candidate vartype.
				Node abstractNode = nodeCreator.transformAbstractNode(candidateNode);
				
				 // Create new constructors for nullNode and realNode and update controlflow of all nodes.
				nodeCreator.createConstructors(abstractNode, realNode, nullNode);
			
				// Create new methods and change alignments.
				MethodTransformer methtransformer = new MethodTransformer(dbService, propertyContainer);
				methtransformer.transformMethods(abstractNode, nullNode, realNode);
				
				/* At this point the candidate has been transformed using the Null Object Pattern.
				 * Now we need to update field assigments and constructor calls. */
				
				/*
				 * Transform candidate fields.
				 */
				for (Map.Entry<String, Node> distinctCandidateField : distinctCandidateFields.entrySet()){
					
					/*
					 * Get vartype of the candidate.
					 */
					String candidateFieldVartype = "";
					Node callerClass = null;
					candidateFqn = propertyContainer.getCandidateFqn();
					
					candidateFieldVartype = (String) distinctCandidateField.getValue().getProperty(SDGPropertyKey.VARTYPE);
					callerClass = distinctCandidateField.getValue()
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
					} else if (callerClass == null) {
						System.err.println("No valid main class found.");
						printNode(distinctCandidateField.getValue());
						return;
					} else {
						System.out.println("Started transforming the field " + candidateFieldVartype + " contained in " 
								+ callerClass.getProperty(SDGPropertyKey.FQN) + ".");
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
					System.out.println("Found [" + candidateConditionAssignments.size() + "] "
							+ "condition nodes belonging to fields of this candidate in this class.");
					
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
			
				ResourceIterator<Node> similarFields = dbService.findNodes(
						SDGLabel.FIELD, 
						SDGPropertyKey.VARTYPE,
						candidateFqn);
				List<Long> callerClassIds = new ArrayList<>();
				
				while (similarFields.hasNext()){
					Node similarField = similarFields.next();
					similarField.setProperty(SDGPropertyKey.VARTYPE, propertyContainer.getAbstractFqn());
					updateFieldAssigments(similarField, propertyContainer.getAbstractFqn());
					
					/*
					 * Add all mainClasses containing a candidate field to mainClass ids.
					 */
					Node callerClass = similarField.getSingleRelationship(
							RelTypes.CONTAINS_FIELD, 
							Direction.INCOMING)
							.getStartNode();
					long callerClassId = callerClass.getId();
					if (!callerClassIds.contains(callerClassId)){
						callerClassIds.add(callerClassId);
					}
				}
				similarFields.close();
				
				/*
				 * Find uninitialized fields of type abstractFqn for every class in mainClassIds an initialize them.
				 */
				for (long id : callerClassIds){
					Node callerClass = dbService.getNodeById(id);
					findAndInitializeUninitializedFields(callerClass, id, propertyContainer.getAbstractFqn(), nullNode);
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
	 * @param callerClass
	 * @param callerClassId
	 * @param vartype
	 * @param nullNode
	 */
	private void findAndInitializeUninitializedFields(Node callerClass, long callerClassId, String vartype, Node nullNode){
		
		String uninitializedFieldQuery = QueryReader.readQuery(QueryPaths.uninitializedFieldQueryPath);
		Map<String, Object> params = new HashMap<>();
		params.put("vartype", vartype);
		params.put("callerClassId", callerClassId);
			
		Result result = dbService.execute(uninitializedFieldQuery, params);
		
		ResourceIterator<Node> fields = result.columnAs("field");
		List<Node> constructors = new ArrayList<Node>();
		for (Relationship rel : callerClass.getRelationships(RelTypes.CONTAINS_CONSTRUCTOR, Direction.OUTGOING)){
			constructors.add(rel.getEndNode());
		}
		
		String mainFqn = (String) callerClass.getProperty(SDGPropertyKey.FQN);
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
			
			System.out.println("\n" + node.toString() + ":");
			for (String property : node.getPropertyKeys()){
				System.out.printf("%s = %s%n", property, node.getProperty(property));
			}
			System.out.println();
			tx.success();
		}
	}
	
	
	/**
	 * Check if match() has been called. This is necessary for transformation methods.
	 */
	private void checkIsMatched(){
		if (!hasMatchedCandidate) {
			System.err.println("Call match() before calling transform().");
			return;
		} else {
			System.out.println("Started transforming ...");
		}
	}
	
		
}
