package com.tnr.neo4j.java.nullobject.transformation;

import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.tnr.neo4j.java.nullobject.util.RelTypes;
import com.tnr.neo4j.java.nullobject.util.SDGLabel;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyValues;

public class ConstructorControlFlowTransformer {
	
	GraphDatabaseService dbService;
	
	
	public ConstructorControlFlowTransformer (GraphDatabaseService dbService) {
		this.dbService = dbService;
	}

	/**
	 * Creates the controlFlow for a new constructor using the super constructor.
	 * This method is to be used inside the transaction of the transform() method.
	 * @param superConstructor the super constructor to call (default should be Object)
	 * @param newConstructor the new constructor
	 * @return
	 */
	protected void createConstructorFlow (Node superConstructor, Node newConstructor, String classFqn){
		
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
	 * This method is to be used inside the transaction of the transform() method.
	 * @param oldConstructor
	 * @param newConstrutctor
	 * @param newFqn the fqn of the new constructor.
	 */
	protected void transferConstructorCalls (Node oldConstructor, Node newConstrutctor, String newFqn){
		
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
	 * This method is to be used inside the transaction of the transform() method.
	 * @param oldConstructor
	 * @param newConstructor
	 * @param newFqn the fqn of the new constructor.
	 */
	protected void transferConstructorFlow(Node oldConstructor, Node newConstructor, String newFqn){
		
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
}
