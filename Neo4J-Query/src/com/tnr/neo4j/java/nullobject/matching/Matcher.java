package com.tnr.neo4j.java.nullobject.matching;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import com.tnr.neo4j.java.nullobject.util.query.QueryPaths;
import com.tnr.neo4j.java.nullobject.util.query.QueryReader;

public class Matcher {
	
	private Map<String, Node> distinctCandidateFields = new HashMap<>();
	private Map<String, Node> distinctConditionAssignments = new HashMap<>();
	private Map<String, Node> distinctCandidates = new HashMap<>();
	
	public Map<String, Node> getDistinctCandidateFields() {
		return distinctCandidateFields;
	}

	public Map<String, Node> getDistinctConditionAssignments() {
		return distinctConditionAssignments;
	}

	public Map<String, Node> getDistinctCandidates() {
		return distinctCandidates;
	}
		
	public boolean match(GraphDatabaseService dbService) {
		
		boolean hasMatched = false;
		System.out.println("Started matching ...");
		long startTime = System.currentTimeMillis();
		/*
		 * Find candidates using the Cypher Match Query
		 */
		Result queryResult =  dbService.execute(QueryReader.readQuery(QueryPaths.matchQueryPath));
		/*
		 * Collect distinct candidates and theirs assignments within if conditions
		 */
		distinctCandidateFields = new HashMap<>();
		distinctConditionAssignments = new HashMap<>();
		distinctCandidates = new HashMap<>();
		
		try (Transaction tx = dbService.beginTx()){
			processResults(queryResult);
			hasMatched = true;
			tx.success();
		}
		
		System.out.println("Found [" + distinctCandidates.size() + "] "
				+ "candidates with [" + distinctCandidateFields.size() + "] candidate fields"
						+ " and [" + distinctConditionAssignments.size() + "] assignments.");
		System.out.println("Finished matching after " + (System.currentTimeMillis() - startTime) + "ms.\n");
		
		return hasMatched;
	}
	
	
	private void processResults(Result queryResult){
		while(queryResult.hasNext()){
			Map<String,Object> result = queryResult.next();
		
			Node fieldNode = (Node) result.get("candidateField");
			Node assignmentNode = (Node) result.get("condVariable");
			Node candidateNode = (Node) result.get("candidate");
	
			String fieldId = String.valueOf(fieldNode.getId());
			String assignmentId = String.valueOf(assignmentNode.getId());
			String candidateId = String.valueOf(candidateNode.getId());
			
			addDistinctCandidateField(fieldId, fieldNode);
			addDistinctCandidateAssignment(assignmentId, assignmentNode);
			addDistinctCandidate(candidateId, candidateNode);
		}
		queryResult.close();
	}
	
	private void addDistinctCandidateField(String id, Node node){
		if (!distinctCandidateFields.containsKey(id)){
			distinctCandidateFields.put(id, node);
		}
	}
	
	private void addDistinctCandidateAssignment(String id, Node node){
		if (!distinctConditionAssignments.containsKey(id)){
			distinctConditionAssignments.put(id, node);
		}
	}
	
	private void addDistinctCandidate(String id, Node node){
		if (!distinctCandidates.containsKey(id)){
			distinctCandidates.put(id, node);
		}
	}
}
