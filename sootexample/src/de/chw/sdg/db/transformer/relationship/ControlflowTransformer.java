package de.chw.sdg.db.transformer.relationship;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.chw.sdg.db.RelTypes;
import soot.ValueBox;
import soot.jimple.IfStmt;
import soot.tagkit.Host;

public class ControlflowTransformer extends GenericRelationshipTransformer {

	private static final String KCASE = "case";

	public ControlflowTransformer() {
		super(RelTypes.CONTROL_FLOW);
	}

	public Relationship transform(final Host sourceUnit, final Host targetUnit, final ValueBox valueBox, final Node sourceNode, final Node targetNode) {
		Relationship relationship = super.transform(sourceNode, targetNode);

		if (sourceUnit instanceof IfStmt) {
			boolean caseValue = ((IfStmt) sourceUnit).getTarget() == targetUnit;
			relationship.setProperty(KCASE, caseValue);
		} else {

		}
		// TODO switch-to-case relationships

		return relationship;
	}

}
