package com.tnr.neo4j.java.nullobject.util;

import org.neo4j.graphdb.RelationshipType;

public enum RelTypes implements RelationshipType{
	AGGREGATED_CALLS, 
	AGGREGATED_FIELD_WRITE, 
	CALLS, 
	CONTAINS_CONSTRUCTOR, 
	CONTAINS_FIELD, 
	CONTAINS_METHOD, 
	CONTAINS_TYPE, 
	CONTAINS_UNIT, 
	CONTROL_FLOW, 
	DATA_FLOW, 
	EXTENDS,
	LAST_UNIT
}
