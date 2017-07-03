package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.jimple.ThrowStmt;

public class ThrowStmtTransformer extends AbstractNodeTransformer {

	// labels
	static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("throwstmt");

	public ThrowStmtTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final ThrowStmt unit) {
		Node node = createNode(TYPE_LABEL);
		node.setProperty(Keys.DISPLAYNAME, unit.toString());
		node.setProperty(Keys.RIGHT_HAND_SIDE_VALUE, unit.getOp().toString());
		return node;
	}

}
