package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;

public class PackageTransformer extends AbstractNodeTransformer {

	private static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("packagedeclaration");

	public PackageTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final String packageName) {
		Node node = createNode(TYPE_LABEL);

		node.setProperty(Keys.DISPLAYNAME, packageName);
		node.setProperty(Keys.NAME, packageName);

		return node;
	}

}
