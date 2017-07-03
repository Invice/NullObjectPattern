package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.slf4j.LoggerFactory;



import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.javaToJimple.jj.extension.LoopIdTag;
import soot.javaToJimple.jj.extension.VariableDeclarationTag;
import soot.jimple.NopStmt;
import soot.tagkit.Tag;

public class NopTransformer extends AbstractNodeTransformer {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NopTransformer.class);

	private static final String HIGHER_LEVEL_STRUCTURE_TAG = "HigherLevelStructureTag";
	private static final String LOOP_ID_TAG = "LoopIdTag";
	private static final String VARIABLE_DECLARATION_NAME = VariableDeclarationTag.INSTANCE.getName();

	// labels
	static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("nop");

	public NopTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final NopStmt unit) {
		Node node = createNode(TYPE_LABEL);

		node.setProperty(Keys.DISPLAYNAME, unit.toString());

		for (Tag tag : unit.getTags()) {
			LOGGER.debug("tag: {}", tag.getName());
		}

		if (unit.hasTag(HIGHER_LEVEL_STRUCTURE_TAG)) {
			Tag tag = unit.getTag(HIGHER_LEVEL_STRUCTURE_TAG);
			node.setProperty(Keys.NOP_KIND, tag.toString());
		}

		if (unit.hasTag(LOOP_ID_TAG)) {
			LoopIdTag tag = (LoopIdTag) unit.getTag(LOOP_ID_TAG);
			node.setProperty(Keys.LOOP_ID, tag.getLoopId());
		}

		if (unit.hasTag(VARIABLE_DECLARATION_NAME)) {
			node.setProperty(Keys.IS_DECLARATION, Boolean.TRUE);
		}

		return node;
	}

}
