package de.chw.sdg;

import java.io.IOException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.LoggerFactory;

import de.chw.sdg.db.Neo4Jdb;
import de.chw.sdg.soot.transformation.MethodBodyTransformer;
import de.chw.sdg.soot.transformation.PackageClassFieldTransformer;
import de.chw.sdg.soot.visitor.Neo4JVisitor;
import soot.PackManager;
import soot.Transform;

public class Sdg {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Sdg.class);

	private final String databaseName;

	private GraphDatabaseService graphDatabaseService;

	public Sdg(final String databaseName) {
		this.databaseName = databaseName;

		setSystemPropertyIfNotSet("skipGoto", "true");
		setSystemPropertyIfNotSet("soot.jimple.validation.JimpleTrapValidator.activated", "false");
		setSystemPropertyIfNotSet("alwaysProduceEndBranchStmt", "true");
		setSystemPropertyIfNotSet("alwaysProduceEndForStmt", "true");
		setSystemPropertyIfNotSet("alwaysProduceEndWhileStmt", "true");
		setSystemPropertyIfNotSet("alwaysProduceEndDoWhileStmt", "true");
	}

	public static void main(final String[] args) throws IOException {
		String classToAnalyze = args[args.length - 1];
		String databaseName = "databases/" + classToAnalyze;

		Sdg sdg = new Sdg(databaseName);
		sdg.connect();
		try {
			sdg.deleteContent();
			sdg.transform(args);
		} finally {
			sdg.shutdown();
		}
	}

	public void connect() {
		graphDatabaseService = new Neo4Jdb(databaseName).getGraphDatabaseService();
	}

	public void deleteContent() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r");
	}

	public void shutdown() {
		graphDatabaseService.shutdown();
	}

	public GraphDatabaseService getGraphDatabaseService(){
		return graphDatabaseService;
	}

	public void transform(final String[] args) {
		final Neo4JVisitor visitor = new Neo4JVisitor(graphDatabaseService);

		final Transform transformPkgClsFld = new Transform("wjap.preCreation",
				new PackageClassFieldTransformer(graphDatabaseService, visitor));
		PackManager.v().getPack("wjap").add(transformPkgClsFld);

		// jtp/tag follows the wjap phase. Hence, we can assume that all application classes and fields are created
		// at this point.
		// PackManager.v().getPack("stp").add(new Transform("stp.bodyTransformation", new BodyTransformer() {

		final Transform transformMethodBody = new Transform("jtp.bodyTransformation",
				new MethodBodyTransformer(graphDatabaseService, visitor));

		PackManager.v().getPack("jtp").add(transformMethodBody);

		// are not executed: jb, jj
		// PackManager.v().getPack("jj").add(new Transform("jj.nop", new BodyTransformer() {
		//
		// @Override
		// protected void internalTransform(final Body b, final String phaseName, final Map<String, String> options)
		// {
		// System.out.println("Executing " + phaseName);
		// Iterator<Unit> iterator = b.getUnits().snapshotIterator();
		// while (iterator.hasNext()) {
		// Unit unit = iterator.next();
		// if (unit instanceof NopStmt) {
		// System.out.println("NoOp detected: " + unit);
		// }
		// }
		// }
		// }));

		// Options.v().set_output_dir(outputDir);
		// InitialResolver.v().setJBBFactory(new MyJimpleBodyBuilderFactory());

		try {
			// start soot with its arguments and calculate the call-graph
			soot.Main.main(args);

			try (Transaction transaction = graphDatabaseService.beginTx()) {
				visitor.executeInterProceduralOperations();

				LOGGER.info("Committing transaction...");
				transaction.success();
				LOGGER.info("Closing transaction...");
			}
		} finally {
			PackManager.v().getPack("wjap").remove("wjap.preCreation");
			PackManager.v().getPack("jtp").remove("jtp.bodyTransformation");
		}

		// for (Pack pack : PackManager.v().allPacks()) {
		// System.out.println("pack: " + pack.toString() + ", phase: " + pack.getPhaseName() + ", options: "
		// + pack.getDeclaredOptions());
		// Iterator<Transform> iterator = pack.iterator();
		// while (iterator.hasNext()) {
		// Transform transform = iterator.next();
		// Map<String, String> options = PhaseOptions.v().getPhaseOptions(transform.getPhaseName());
		// System.out.println("\ttransformation: " + transform.getPhaseName() + ", options: " + options);
		// // + transform.getDeclaredOptions());
		// }
		// }

		// Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();
		// for (SootClass sootClass : applicationClasses) {
		// List<SootMethod> methods = sootClass.getMethods();
		// for (SootMethod sootMethod : methods) {
		// Body body = sootMethod.getActiveBody(); // -> no active body present
		// System.out.println("body: " + body);
		// }
		// }

		LOGGER.info("End of main()");
	}

	private static void setSystemPropertyIfNotSet(final String key, final String value) {
		String readValue = System.getProperty(key);
		if (readValue == null) {
			System.setProperty(key, value);
		}
	}

}
