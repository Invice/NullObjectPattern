package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.ModifierUtil;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.SootField;

public class FieldTransformer extends AbstractNodeTransformer {

	private static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("fielddeclaration");

	public FieldTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final SootField unit) {
		Node node = createNode(TYPE_LABEL);

		node.setProperty(Keys.NAME, unit.getName());
		// custom properties
		node.setProperty(Keys.VAR_TYPE, unit.getType().toString());

		int modifiers = unit.getModifiers();
		ModifierUtil.setModifiers(node, modifiers);

		return node;
	}
}
