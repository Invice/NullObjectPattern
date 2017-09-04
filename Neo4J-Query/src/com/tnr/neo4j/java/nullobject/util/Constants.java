package com.tnr.neo4j.java.nullobject.util;

public class Constants {
	public static final String GraphDatabaseLocation = "C:\\Users\\Tim-Niklas Reck\\Desktop\\Bachelorarbeit\\sootexample\\databases\\";
	public static final String GraphDatabaseName = "de.tnr.sdg.example.cache.MainClass";
	
	public static final String MATCH_QUERY = ""
			+ "MATCH (mainClass:Class)-[:CONTAINS_FIELD]->(candidateField:Field {isfinal:false})<-[:AGGREGATED_FIELD_READ]-(method:Method)\n\u0009"
				+ "USING INDEX candidateField:Field(isfinal)\n"
			+ "MATCH (candidateField)-[:DATA_FLOW]->(condVariable:Assignment)-[:DATA_FLOW]->(condition:Condition {operation:\"!=\"})\n\u0009"
				+ "WHERE condition.operand1 = \"null\" "
					+ "OR condition.operand2 = \"null\"\n"
			+ "MATCH (condVariable)<-[:CONTROL_FLOW]-(ifStmt:NopStmt)  \n\u0009"
				+ "WHERE ifStmt.nopkind = \"IF_COND\" "
					+ "OR ifStmt.nopkind = \"IF_COND_X\" OR (ifStmt) <-[:CONTROL_FLOW]- (:Condition)\n"
			+ "MATCH (candidate:Class)\n\u0009"
				+ "WHERE candidate.fqn = candidateField.vartype\n"
			+ "RETURN DISTINCT candidateField, condVariable, candidate";
}