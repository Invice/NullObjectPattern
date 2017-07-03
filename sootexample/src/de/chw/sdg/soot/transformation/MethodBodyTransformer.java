package de.chw.sdg.soot.transformation;

import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.LoggerFactory;

import de.chw.sdg.soot.traverser.MethodBodyTraverser;
import de.chw.sdg.soot.visitor.Neo4JVisitor;
import soot.Body;
import soot.BodyTransformer;

public class MethodBodyTransformer extends BodyTransformer {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MethodBodyTransformer.class);

	private final GraphDatabaseService graphDatabaseService;
	private final Neo4JVisitor visitor;

	public MethodBodyTransformer(GraphDatabaseService graphDatabaseService, Neo4JVisitor visitor) {
		super();
		this.graphDatabaseService = graphDatabaseService;
		this.visitor = visitor;
	}

	@Override
	protected synchronized void internalTransform(final Body b, final String phaseName, final Map<String, String> options) {
		LOGGER.trace("[" + phaseName + "] " + "options: " + options + "body: " + b.getMethod().getSignature());

		try (Transaction transaction = graphDatabaseService.beginTx()) {

			Body jimpleBody = b.getMethod().getActiveBody();
			MethodBodyTraverser.INSTANCE.traverse(jimpleBody, visitor);
			// MethodBodyTraverser.INSTANCE.traverse(b, logVisitor);

			transaction.success();
		}
	}
}
