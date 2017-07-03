package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;

public abstract class AbstractNodeTransformer {

//	protected static final String KDISPLAYNAME = NamingUtil.INSTANCE.getPropertyKey("displayname");
//	protected static final String KNAME = NamingUtil.INSTANCE.getPropertyKey("name");
//	protected static final String KTYPE = NamingUtil.INSTANCE.getPropertyKey("type");
//	protected static final String KFQN = NamingUtil.INSTANCE.getPropertyKey("fqn");

	private final GraphDatabaseService graphDatabaseService;

	protected AbstractNodeTransformer(final GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}

	protected Node createNode() {
		Node node = graphDatabaseService.createNode();
		return node;
	}

	protected Node createNode(final Label label) {
		Node node = graphDatabaseService.createNode(label);
		node.setProperty(Keys.TYPE, label.name());
		return node;
	}

}
