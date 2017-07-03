package de.chw.sdg.soot.transformation;

import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.LoggerFactory;

import de.chw.sdg.soot.visitor.Neo4JVisitor;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;

public class PackageClassFieldTransformer extends SceneTransformer {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PackageClassFieldTransformer.class);

	private final GraphDatabaseService graphDatabaseService;
	private final Neo4JVisitor visitor;

	public PackageClassFieldTransformer(GraphDatabaseService graphDatabaseService, Neo4JVisitor visitor) {
		super();
		this.graphDatabaseService = graphDatabaseService;
		this.visitor = visitor;
	}

	@Override
	protected void internalTransform(final String phaseName, final Map<String, String> options) {
		LOGGER.info("[" + phaseName + "] " + "options: " + options);

		try (Transaction transaction = graphDatabaseService.beginTx()) {

			// for (SootClass clazz : Scene.v().getLibraryClasses()) { // e.g., java.lang.Object
			// visitor.visit(clazz.getPackageName());
			// visitor.visit(clazz);
			// // for (SootField field : clazz.getFields()) {
			// // visitor.visit(field);
			// // }
			// }
			for (SootClass clazz : Scene.v().getApplicationClasses()) {
				visitor.visit(clazz.getPackageName());
				visitor.visit(clazz);
				for (SootField field : clazz.getFields()) {
					visitor.visit(field);
				}
			}
			// e.g., System.out is of type java.io.PrintStream
			// for (SootClass clazz : Scene.v().getPhantomClasses()) {
			// visitor.visit(clazz.getPackageName());
			// visitor.visit(clazz);
			// // for (SootField field : clazz.getFields()) {
			// // visitor.visit(field);
			// // }
			// }

			// new CallGraphBuilder(pa)

			transaction.success();

		}
	}
}
