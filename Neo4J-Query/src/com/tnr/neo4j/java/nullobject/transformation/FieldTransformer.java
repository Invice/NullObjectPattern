package com.tnr.neo4j.java.nullobject.transformation;

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

import com.tnr.neo4j.java.nullobject.transformation.util.PropertyContainer;
import com.tnr.neo4j.java.nullobject.util.RelTypes;
import com.tnr.neo4j.java.nullobject.util.SDGLabel;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;
import com.tnr.neo4j.java.nullobject.util.SDGPropertyValues;
import com.tnr.neo4j.java.nullobject.util.StringUtil;
import com.tnr.neo4j.java.nullobject.util.query.QueryPaths;
import com.tnr.neo4j.java.nullobject.util.query.QueryReader;

public class FieldTransformer {
	
	private GraphDatabaseService dbService;
	private PropertyContainer propertyContainer;
	
	public FieldTransformer(final GraphDatabaseService dbService, PropertyContainer propertyContainer) {
		this.dbService = dbService;
		this.propertyContainer = propertyContainer;
	}
	
	public void updateFields(Node nullNode){
		ResourceIterator<Node> similarFields = dbService.findNodes(
				SDGLabel.FIELD, 
				SDGPropertyKey.VARTYPE,
				propertyContainer.getCandidateFqn());
		List<Long> callerClassIds = updateSimilarFields(similarFields);
		
		/*
		 * Find uninitialized fields of type abstractFqn for every class in mainClassIds an initialize them.
		 */
		initFields(callerClassIds, nullNode);
	}
	
	private List<Long> updateSimilarFields(ResourceIterator<Node> similarFields){
		
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
		return callerClassIds;
	}
	
	private void initFields(List<Long> callerClassIds, Node nullNode){
		for (long id : callerClassIds){
			Node callerClass = dbService.getNodeById(id);
			findAndInitializeUninitializedFields(callerClass, id, propertyContainer.getAbstractFqn(), nullNode);
		}
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
		
		setTempAssignProperties(tempAssign, var, nullFqn);
		setInitProperties(init, var, nullFqn);
		setFieldProperties(field, fieldAssign, var, mainFqn);

	}
	
	private void setTempAssignProperties(Node tempAssign, String var, String nullFqn){
		String rightValue = "new " + nullFqn;
		tempAssign.setProperty(SDGPropertyKey.VARTYPE, nullFqn);
		tempAssign.setProperty(SDGPropertyKey.VAR, var);
		tempAssign.setProperty(SDGPropertyKey.RIGHTVALUE, rightValue);
		tempAssign.setProperty(SDGPropertyKey.DISPLAYNAME , var + " = " + rightValue);
		tempAssign.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_ASSIGNMENT);
		tempAssign.setProperty(SDGPropertyKey.OPERATION, "new");
	}
	
	private void setInitProperties(Node init, String var, String nullFqn){
	
		init.setProperty(SDGPropertyKey.ARGS, new String[0]);
		init.setProperty(SDGPropertyKey.CALLER, var);
		init.setProperty(SDGPropertyKey.FQN, nullFqn + ".<init>()");
		init.setProperty(SDGPropertyKey.ARGUMENTSCOUNT, 0);
		init.setProperty(SDGPropertyKey.DISPLAYNAME, "<init>()");
		init.setProperty(SDGPropertyKey.RETURNTYPE, "void");
		init.setProperty(SDGPropertyKey.NAME, "<init>");
		init.setProperty(SDGPropertyKey.TYPE, SDGPropertyValues.TYPE_CONSTRUCTORCALL);
		
	}
	
	private void setFieldProperties(Node field, Node fieldAssign, String var, String mainFqn){
		
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
	

}
