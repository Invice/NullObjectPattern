package com.tnr.neo4j.java.nullobject.transformation;

import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.tnr.neo4j.java.nullobject.transformation.util.PropertyContainer;
import com.tnr.neo4j.java.nullobject.util.Constants;
import com.tnr.neo4j.java.nullobject.util.RelTypes;
import com.tnr.neo4j.java.nullobject.util.SDGLabel;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;
import com.tnr.neo4j.java.nullobject.util.StringUtil;

public class NodeCreator {

	private GraphDatabaseService dbService;
	private Map<String, Object> candidateProperties;
	
	private String realPrefix;
	private String abstractPrefix;
	private String nullPrefix;
	
	private String realFqn;
	private String nullFqn;
	private String abstractFqn;
	
	public NodeCreator(GraphDatabaseService dbService, PropertyContainer propertyContainer){
		this.dbService = dbService;
		this.candidateProperties = propertyContainer.getCandidateProperties();
		
		realPrefix = propertyContainer.getRealPrefix();
		abstractPrefix = propertyContainer.getAbstractPrefix();
		nullPrefix = propertyContainer.getNullPrefix();
		
		realFqn = StringUtil.addPrefixToClass(realPrefix, candidateProperties.get(SDGPropertyKey.FQN));
		nullFqn = StringUtil.addPrefixToClass(nullPrefix, candidateProperties.get(SDGPropertyKey.FQN));	
		abstractFqn = StringUtil.addPrefixToClass(abstractPrefix, candidateProperties.get(SDGPropertyKey.FQN));
	}
	
	
	/**
	 * Create realNode.
	 */
	public Node createRealNode(Node packageNode, Node candidateNode) {
		
		System.out.println("Creating " + realFqn + ".");
		Node realNode = dbService.createNode(SDGLabel.CLASS);
		realNode.setProperty(SDGPropertyKey.FQN, realFqn);
		realNode.setProperty(SDGPropertyKey.DISPLAYNAME, realPrefix+candidateProperties.get(SDGPropertyKey.DISPLAYNAME));
		realNode.setProperty(SDGPropertyKey.NAME, realPrefix+candidateProperties.get(SDGPropertyKey.NAME));
		realNode.setProperty(SDGPropertyKey.VISIBILITY, candidateProperties.get(SDGPropertyKey.VISIBILITY));
		realNode.setProperty(SDGPropertyKey.ORIGIN, candidateProperties.get(SDGPropertyKey.ORIGIN));
		realNode.setProperty(SDGPropertyKey.TYPE, candidateProperties.get(SDGPropertyKey.TYPE));
		realNode.setProperty(SDGPropertyKey.ISABSTRACT, "false");
		realNode.setProperty(SDGPropertyKey.TRANSFORMED, "true");	
		
		realNode.createRelationshipTo(candidateNode, RelTypes.EXTENDS);
		packageNode.createRelationshipTo(realNode, RelTypes.CONTAINS_TYPE);
		
		return realNode;
	}
	
	/**
	 * Create nullNode.
	 */
	public Node createNullNode(Node packageNode, Node candidateNode) {
		
		System.out.println("Creating " + nullFqn + ".");
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
		
		return nullNode;
	}
	
	/**
	 * Change node to abstractNode and update candidate vartype.
	 */
	public void createAbstractNode (Node candidateNode) {
		
		System.out.println("Creating " + abstractFqn + ".");
		candidateNode.setProperty(SDGPropertyKey.FQN, abstractFqn);
		candidateNode.setProperty(SDGPropertyKey.DISPLAYNAME, abstractPrefix + candidateProperties.get(SDGPropertyKey.DISPLAYNAME));
		candidateNode.setProperty(SDGPropertyKey.NAME, abstractPrefix + candidateProperties.get(SDGPropertyKey.NAME));
		candidateNode.setProperty(SDGPropertyKey.VARTYPE, abstractFqn);
		candidateNode.setProperty(SDGPropertyKey.ISABSTRACT, "true");
		candidateNode.setProperty(SDGPropertyKey.TRANSFORMED, "true");
	}
	
	/**
	 * Creates the Constructors for abstract, real and null node.
	 * @param abstractNode
	 * @param realNode
	 * @param nullNode
	 */
	public void createConstructors(Node abstractNode, Node realNode, Node nullNode){
		/*
		 * Create new constructors for nullNode and realNode.
		 */
		// TODO: multiple (overloaded) constructors.
		Node nullConstructorNode = dbService.createNode(SDGLabel.CONSTRUCTOR);
		Node realConstructorNode = dbService.createNode(SDGLabel.CONSTRUCTOR);
		ConstructorControlFlowTransformer cflowtransformer = new ConstructorControlFlowTransformer(dbService);
		
		Iterable<Relationship> classOutRels = abstractNode.getRelationships(RelTypes.CONTAINS_CONSTRUCTOR, Direction.OUTGOING);
		for (Relationship rel : classOutRels){
			Node constructorNode = rel.getEndNode();
			
			//Transfer old constructor calls to the new constructor for the real node.
			cflowtransformer.transferConstructorCalls(constructorNode, realConstructorNode, realFqn);
			
			//Transfer old constructor control flow to real constructor.
			cflowtransformer.transferConstructorFlow(constructorNode, realConstructorNode, realFqn);
			
			realNode.createRelationshipTo(realConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
			
			cflowtransformer.createConstructorFlow(constructorNode, nullConstructorNode, nullFqn);
			nullNode.createRelationshipTo(nullConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
			
			Relationship superCall = constructorNode.getSingleRelationship(RelTypes.AGGREGATED_CALLS, Direction.OUTGOING);
			Node superConstructor = superCall.getEndNode();
			superCall.delete();
			cflowtransformer.createConstructorFlow(superConstructor, constructorNode, abstractFqn);

			break;
		}
	}
	
	
	
	
}


