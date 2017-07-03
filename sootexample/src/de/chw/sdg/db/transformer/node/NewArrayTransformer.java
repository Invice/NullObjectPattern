package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.jimple.NewArrayExpr;

class NewArrayTransformer extends AbstractNodeTransformer {

	private static final String KOPERATION = NamingUtil.INSTANCE.getPropertyKey("operation");
	private static final String KBASE_TYPE = NamingUtil.INSTANCE.getPropertyKey("basetype");
	private static final String KSIZE = NamingUtil.INSTANCE.getPropertyKey("size");

	protected NewArrayTransformer(GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	void transform(Node node, NewArrayExpr value) {
		node.setProperty(KOPERATION, "newarray");
		node.setProperty(KBASE_TYPE, value.getBaseType().toString());
		node.setProperty(KSIZE, value.getSize().toString());
	}

}
