package com.tnr.neo4j.java.nullobject.transformation;

import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.tnr.neo4j.java.nullobject.util.Constants;
import com.tnr.neo4j.java.nullobject.util.RelTypes;
import com.tnr.neo4j.java.nullobject.util.SDGLabel;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;
import com.tnr.neo4j.java.nullobject.util.StringUtil;

public class NodeCreator {

	private GraphDatabaseService dbService;
	Map<String,Object> candidateProperties;
	
	String realPrefix;
	String abstractPrefix;
	String nullPrefix;
	
	String realFqn;
	String nullFqn;
	String abstractFqn;
	
	
	
	public NodeCreator(GraphDatabaseService dbService, Map<String,Object> candidateProperties){
		this.dbService = dbService;
		this.candidateProperties = candidateProperties;
		
		realPrefix = Constants.realPrefix;
		abstractPrefix = Constants.abstractPrefix;
		nullPrefix = Constants.nullPrefix;
		
		realFqn = StringUtil.addPrefixToClass(realPrefix, candidateProperties.get(SDGPropertyKey.FQN));
		nullFqn = StringUtil.addPrefixToClass(nullPrefix, candidateProperties.get(SDGPropertyKey.FQN));	
		abstractFqn = StringUtil.addPrefixToClass(abstractPrefix, candidateProperties.get(SDGPropertyKey.FQN));
	}
	
	/**
	 * Create realNode.
	 */
	public Node createRealNode() {
		
		Node realNode = dbService.createNode(SDGLabel.CLASS);
		realNode.setProperty(SDGPropertyKey.FQN, realFqn);
		realNode.setProperty(SDGPropertyKey.DISPLAYNAME, realPrefix+candidateProperties.get(SDGPropertyKey.DISPLAYNAME));
		realNode.setProperty(SDGPropertyKey.NAME, realPrefix+candidateProperties.get(SDGPropertyKey.NAME));
		realNode.setProperty(SDGPropertyKey.VISIBILITY, candidateProperties.get(SDGPropertyKey.VISIBILITY));
		realNode.setProperty(SDGPropertyKey.ORIGIN, candidateProperties.get(SDGPropertyKey.ORIGIN));
		realNode.setProperty(SDGPropertyKey.TYPE, candidateProperties.get(SDGPropertyKey.TYPE));
		realNode.setProperty(SDGPropertyKey.ISABSTRACT, "false");
		realNode.setProperty(SDGPropertyKey.TRANSFORMED, "true");	
		
		return realNode;
	}
	
	/**
	 * Create nullNode.
	 */
	public Node createNullNode() {
		Node nullNode = dbService.createNode(SDGLabel.CLASS);
		
		nullNode.setProperty(SDGPropertyKey.FQN, nullFqn);
		nullNode.setProperty(SDGPropertyKey.DISPLAYNAME, nullPrefix+candidateProperties.get(SDGPropertyKey.DISPLAYNAME));
		nullNode.setProperty(SDGPropertyKey.NAME, nullPrefix+candidateProperties.get(SDGPropertyKey.NAME));
		nullNode.setProperty(SDGPropertyKey.VISIBILITY, candidateProperties.get(SDGPropertyKey.VISIBILITY));
		nullNode.setProperty(SDGPropertyKey.ORIGIN, candidateProperties.get(SDGPropertyKey.ORIGIN));
		nullNode.setProperty(SDGPropertyKey.TYPE, candidateProperties.get(SDGPropertyKey.TYPE));
		nullNode.setProperty(SDGPropertyKey.ISABSTRACT, "false");
		nullNode.setProperty(SDGPropertyKey.TRANSFORMED, "true");
		
		return nullNode;
	}
	
	/**
	 * Change node to abstractNode and update candidate vartype.
	 */
	public void createAbstractNode (Node candidateNode) {
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
		ConstructorControlFlowTransformer cflowhandler = new ConstructorControlFlowTransformer(dbService);
		
		Iterable<Relationship> classOutRels = abstractNode.getRelationships(RelTypes.CONTAINS_CONSTRUCTOR, Direction.OUTGOING);
		for (Relationship rel : classOutRels){
			Node constructorNode = rel.getEndNode();
			
			//Transfer old constructor calls to the new constructor for the real node.
			cflowhandler.transferConstructorCalls(constructorNode, realConstructorNode, realFqn);
			
			//Transfer old constructor control flow to real constructor.
			cflowhandler.transferConstructorFlow(constructorNode, realConstructorNode, realFqn);
			
			realNode.createRelationshipTo(realConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
			
			cflowhandler.createConstructorFlow(constructorNode, nullConstructorNode, nullFqn);
			nullNode.createRelationshipTo(nullConstructorNode, RelTypes.CONTAINS_CONSTRUCTOR);
			
			Relationship superCall = constructorNode.getSingleRelationship(RelTypes.AGGREGATED_CALLS, Direction.OUTGOING);
			Node superConstructor = superCall.getEndNode();
			superCall.delete();
			cflowhandler.createConstructorFlow(superConstructor, constructorNode, abstractFqn);

			break;
		}
	}
	
	
	
	
}


