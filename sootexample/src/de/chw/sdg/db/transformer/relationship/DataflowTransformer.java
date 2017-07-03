package de.chw.sdg.db.transformer.relationship;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.chw.sdg.db.RelTypes;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.Value;
import soot.ValueBox;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.tagkit.Host;

public class DataflowTransformer extends GenericRelationshipTransformer {

	static enum Operand {
		LEFT, RIGHT, UNKNOWN
	}

	private static final String KVAR = "var";
	private static final String KOPERAND = NamingUtil.INSTANCE.getPropertyKey("operand");

	public DataflowTransformer() {
		super(RelTypes.DATA_FLOW);
	}

	public Relationship transform(final Host sourceUnit, final Host targetUnit, final ValueBox variable,
			final Node sourceNode, final Node targetNode) {
		Relationship relationship = super.transform(sourceNode, targetNode);

		String variableName = variable.getValue().toString();
		relationship.setProperty(KVAR, variableName);

		if (targetUnit instanceof DefinitionStmt) {
			DefinitionStmt definitionStmt = (DefinitionStmt) targetUnit;
			Value rightOp = definitionStmt.getRightOp();
			setPropertyKeyOperand(variable, relationship, rightOp);
		} else if (targetUnit instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) targetUnit;
			Value condition = ifStmt.getCondition();
			setPropertyKeyOperand(variable, relationship, condition);
		}

		return relationship;
	}

	private void setPropertyKeyOperand(final ValueBox variable, Relationship relationship, Value expression) {
		if (expression instanceof BinopExpr) { // or ConditionExpr
			BinopExpr binOp = (BinopExpr) expression;
			Operand operand;
			if (variable.getValue().equals(binOp.getOp1())) {
				operand = Operand.LEFT;
			} else if (variable.getValue().equals(binOp.getOp2())) {
				operand = Operand.RIGHT;
			} else {
				operand = Operand.UNKNOWN;
			}
			relationship.setProperty(KOPERAND, operand.toString());
		}
	}

}
