package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;
import soot.Unit;

public class UnitTransformer extends AbstractNodeTransformer {

	public UnitTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final Unit unit) {
		Node node = createNode();

		node.setProperty(Keys.DISPLAYNAME, unit.toString());
		node.setProperty(Keys.NAME, unit.toString());
		node.setProperty(Keys.TYPE, unit.getClass().toString());

		return node;
	}

}
