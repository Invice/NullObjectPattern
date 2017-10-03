package com.tnr.neo4j.java.nullobject.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.tnr.neo4j.java.nullobject.transformation.util.PropertyContainer;
import com.tnr.neo4j.java.nullobject.util.RelTypes;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;

public class CandidateFieldTransformer {
	
	private PropertyContainer propertyContainer;
	
	public CandidateFieldTransformer(PropertyContainer propertyContainer) {
		this.propertyContainer = propertyContainer;
	}
	
	public void asd(Map<String,Node> distinctCandidateFields, Map<String,Node> distinctConditionAssignments){
		for (Map.Entry<String, Node> distinctCandidateField : distinctCandidateFields.entrySet()){
			
			/*
			 * Get vartype of the candidate.
			 */
			String candidateFieldVartype = "";
			String candidateFieldName = "";
			Node callerClass = null;
			String candidateFqn = propertyContainer.getCandidateFqn();
			
			candidateFieldVartype = (String) distinctCandidateField.getValue().getProperty(SDGPropertyKey.VARTYPE);
			candidateFieldName = (String) distinctCandidateField.getValue().getProperty(SDGPropertyKey.NAME);
			
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
				System.out.println("Started transforming the field " + candidateFieldVartype + " " + candidateFieldName + " contained in " 
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
			System.out.println("\n" + node.toString() + ":");
			for (String property : node.getPropertyKeys()){
				System.out.printf("%s = %s%n", property, node.getProperty(property));
			}
			System.out.println();
	}
}
