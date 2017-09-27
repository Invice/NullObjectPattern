package com.tnr.neo4j.java.nullobject.transformation;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

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
 * "Jesse... let it go... we need to cook." - WW
 */
public class MethodTransformer {
	
	private GraphDatabaseService dbService;

	private String realPrefix;
	private String abstractPrefix;
	private String nullPrefix;
	
	private String realFqn;
	private String nullFqn;
	private String abstractFqn;
	
	public MethodTransformer (GraphDatabaseService dbService, PropertyContainer propertyContainer) {
		this.dbService = dbService;
		
		realPrefix = propertyContainer.getRealPrefix();
		abstractPrefix = propertyContainer.getAbstractPrefix();
		nullPrefix = propertyContainer.getNullPrefix();
		
		realFqn = StringUtil.addPrefixToClass(realPrefix, propertyContainer.getCandidateProperties().get(SDGPropertyKey.FQN));
		nullFqn = StringUtil.addPrefixToClass(nullPrefix, propertyContainer.getCandidateProperties().get(SDGPropertyKey.FQN));	
		abstractFqn = StringUtil.addPrefixToClass(abstractPrefix, propertyContainer.getCandidateProperties().get(SDGPropertyKey.FQN));
	}
	
	public void transformMethods(Node candidateNode, Node nullNode, Node realNode){
		
		long candidateId = candidateNode.getId();
		String methodQuery = QueryReader.readQuery(QueryPaths.methodQueryPath);
		Map<String,Object> params = new HashMap<>();
		params.put("candidateId", candidateId);
		
		Result methodQueryResult = dbService.execute(methodQuery, params);
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
}
