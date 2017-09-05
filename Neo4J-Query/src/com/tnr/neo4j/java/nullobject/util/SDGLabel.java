package com.tnr.neo4j.java.nullobject.util;

import org.neo4j.graphdb.Label;

/**
 * This class contains all labels that are used in the Java2Neo4j representation of a Java program.
 * @author Tim-Niklas Reck
 *
 */
public class SDGLabel {
	
	public static final Label ASSIGNMENT = Label.label(SDGPropertyValues.TYPE_ASSIGNMENT);
	public static final Label CLASS = Label.label(SDGPropertyValues.TYPE_CLASS);
	public static final Label CONDITION = Label.label(SDGPropertyValues.TYPE_CONDITION);
	public static final Label CONSTRUCTOR = Label.label(SDGPropertyValues.TYPE_CONSTRUCTOR);
	public static final Label CONSTRUCTORCALL = Label.label(SDGPropertyValues.TYPE_CONSTRUCTORCALL);
	public static final Label FIELD = Label.label(SDGPropertyValues.TYPE_FIELD);
	public static final Label METHOD = Label.label(SDGPropertyValues.TYPE_METHOD);
	public static final Label METHODCALL = Label.label(SDGPropertyValues.TYPE_METHODCALL);
	public static final Label METHODCALLWITHRETURNVALUE =  Label.label(SDGPropertyValues.TYPE_METHODCALLWITHRETURNVALUE);
	public static final Label NOPSTMT = Label.label(SDGPropertyValues.TYPE_NOPSTMT);
	public static final Label PACKAGE = Label.label(SDGPropertyValues.TYPE_PACKAGE);
	public static final Label RETURNSTMT = Label.label(SDGPropertyValues.TYPE_RETURNSTMT);
	
}
