package com.tnr.neo4j.java.nullobject.index;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

public class Indexer {
	
	/**
	 * The Neo4J DatabaseService that is managing our current database.
	 */
	private final GraphDatabaseService dbService;
	
	/**
	 * Used by method createIndexes().
	 */
	private Schema schema = null;
	
	public Indexer(GraphDatabaseService dbService) {
		this.dbService = dbService;
		
		try (Transaction tx = dbService.beginTx()) {
		this.schema = dbService.schema();		
		indexes = schema.getIndexes();
			tx.success();
		}		
	}
	
	/**
	 * Used by method createIndexes().
	 */
	private Iterable<IndexDefinition> indexes;
	
	/**
	 * Creates a unique index for a label.
	 * Called by createIndexes().
	 * @param label
	 * @param property
	 */
	public void createUniqueIndex(String label, String property){
		boolean indexed = false;
		
		try(Transaction tx = dbService.beginTx()){
			for (IndexDefinition def : indexes){
				indexed = indexed || (def.getLabel().name().equals(label));
			}
			if (!indexed) {
					schema.indexFor(Label.label(label)).on(property).create();
			}
			tx.success();
		}
	}
	
	/**
	 * Prints the existing indexes to console.
	 */
	public void printIndexes() {
		try (Transaction tx = dbService.beginTx()){
			if (schema == null){
				schema = dbService.schema();
			}
			Iterable<IndexDefinition> indexes = schema.getIndexes();
			
			for (IndexDefinition def : indexes) {
				System.out.print(":"+def.getLabel());
				for (String key : def.getPropertyKeys()){
					System.out.print("(" + key +")");
				}
				System.out.println();
			}
			tx.success();
		} 
	}
	
	
	
	

}
