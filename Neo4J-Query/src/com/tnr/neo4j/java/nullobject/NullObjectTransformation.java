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
import com.tnr.neo4j.java.nullobject.transformation.CandidateFieldTransformer;
import com.tnr.neo4j.java.nullobject.transformation.FieldTransformer;
import com.tnr.neo4j.java.nullobject.transformation.MethodTransformer;
import com.tnr.neo4j.java.nullobject.transformation.NodeCreator;
import com.tnr.neo4j.java.nullobject.transformation.util.PropertyContainer;
import com.tnr.neo4j.java.nullobject.util.GraphDatabaseConstants;
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
		dbService = new GraphDatabaseFactory().newEmbeddedDatabase(new File(GraphDatabaseConstants.GraphDatabaseLocation + GraphDatabaseConstants.cacheDB));
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
				assertUniqueFqn(propertyContainer);
				candidateFqn = propertyContainer.getCandidateFqn();
				
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
				
				// Transform candidate fields.
				CandidateFieldTransformer callTransform = new CandidateFieldTransformer(propertyContainer);
				callTransform.asd(distinctCandidateFields, distinctConditionAssignments);
				
				// Change vartype of all fields with the previous type of the candidate node to the abstract type.
				FieldTransformer fieldTransformer = new FieldTransformer(dbService, propertyContainer);
				fieldTransformer.updateFields(nullNode);
				
				//End of candidate Transaction
				tx.success();
		}
			
			System.out.println("Transformed candidate node: " + candidateFqn + " (" + candidateNode.toString() + ")");
		}	
		System.out.println("Finished transforming.");
		System.out.println("Time spent transforming: " + (System.currentTimeMillis() - startTime) + "ms");
		
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
	
	/*
	 * Asserts that the prefix + fqn combinations for all new classes are available.
	 */
	private PropertyContainer assertUniqueFqn(PropertyContainer propertyContainer) {
		
		boolean fresh = false;
		String nameQuery = QueryReader.readQuery(QueryPaths.nameQueryPath);
		
// 		TestCases:
//		dbService.createNode(SDGLabel.CLASS).setProperty(SDGPropertyKey.FQN, "de.tnr.sdg.example.cache.RealText");
//		dbService.createNode(SDGLabel.CLASS).setProperty(SDGPropertyKey.FQN, "de.tnr.sdg.example.cache.Null0Text");
		
		while (!fresh) {
			Map<String, Object> params = new HashMap<>();
			params.put("realFqn", propertyContainer.getRealFqn());
			params.put("nullFqn", propertyContainer.getNullFqn());
			params.put("abstractFqn", propertyContainer.getAbstractFqn());
			Result result = dbService.execute(nameQuery, params);
			
			if (result.hasNext()){
				propertyContainer.increasePrefixNum();
			} else {
				fresh = true;
			}
		}
		return propertyContainer;
	}
	
	/**
	 * Calls all methods to transform the sdg in a Neo4J database.
	 */
	public void execute() {
		
		createIndexes();
		printIndexes();
		match();
		transform();
	}
}
