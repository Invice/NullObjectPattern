package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.Unit;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;

public class ReturnTransformer extends AbstractNodeTransformer {

	// labels
	static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("return");

	public ReturnTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final ReturnStmt unit) {
		Node node = createReturnNode(unit);
		node.setProperty(Keys.OPERATION, "value");
		node.setProperty(Keys.RIGHT_HAND_SIDE_VALUE, unit.getOp().toString());
		return node;
	}

	public Node transform(final ReturnVoidStmt unit) {
		Node node = createReturnNode(unit);

		return node;
	}

	private Node createReturnNode(final Unit unit) {
		Node node = createNode(TYPE_LABEL);
		node.setProperty(Keys.DISPLAYNAME, unit.toString());
		return node;
	}

}
