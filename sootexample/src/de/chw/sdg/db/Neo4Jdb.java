package de.chw.sdg.db;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class Neo4Jdb {

	//	private static final Logger LOGGER = LoggerFactory.getLogger(Neo4Jdb.class);

	private final GraphDatabaseService graphDatabaseService;

	/**
	 * Creates a Neo4J database service and registers it as shutdown hook.
	 *
	 * @param databaseName
	 */
	public Neo4Jdb(final String databaseName) {
		File databaseFile = new File(databaseName);
		graphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabase(databaseFile);
		registerShutdownHook(graphDatabaseService);
	}

	public GraphDatabaseService getGraphDatabaseService() {
		return graphDatabaseService;
	}

	private static void registerShutdownHook(final GraphDatabaseService database) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				database.shutdown();
				//				LOGGER.info("Database was shutdown.");
			}
		});
	}
}
