package com.tnr.neo4j.java.nullobject;

import org.neo4j.graphdb.Node;

public interface Transformation {
	
	/**
	 * Matches candidates for further processing.
	 */
	public void match();
	
	/**
	 * Transforms the graphDb using candidates obtained with match().
	 */
	public void transform();
	
	/**
	 * Creates necessary indexes, if they don't already exist.
	 */
	public void createIndexes();
	
	/**
	 * Prints the existing indexes to console.
	 */
	public void printIndexes();
	
	/**
	 * Prints the properties of a node to console.
	 * @param node
	 */
	public void printNode(Node n);
}
