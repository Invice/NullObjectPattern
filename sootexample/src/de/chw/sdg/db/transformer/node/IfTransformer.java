package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.Immediate;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.IfStmt;
import soot.jimple.LengthExpr;
import soot.jimple.NegExpr;

public class IfTransformer extends AbstractNodeTransformer {

	// labels
	private static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("condition");

	public IfTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final IfStmt stmt) {
		Node node = createNode(TYPE_LABEL);
		node.setProperty(Keys.DISPLAYNAME, stmt.getCondition().toString());
		node.setProperty(Keys.NAME, stmt.getCondition().toString());
		// custom properties
		Value condition = stmt.getCondition();
		if (condition instanceof ConditionExpr) {
			ConditionExpr expr = (ConditionExpr) condition;
			String op1 = valueToString(node, expr.getOp1());
			String op2 = valueToString(node, expr.getOp2());

			node.setProperty(Keys.OPERATION, expr.getSymbol().trim());
			node.setProperty(Keys.OPERAND1, op1);
			node.setProperty(Keys.OPERAND2, op2);
		} else if (condition instanceof NegExpr) {
			NegExpr expr = (NegExpr) condition;
			String op1 = valueToString(node, expr.getOp());

			node.setProperty(Keys.OPERATION, "not");
			node.setProperty(Keys.OPERAND1, op1);
		} else if (condition instanceof Immediate) {
			Immediate expr = (Immediate) condition;

			node.setProperty(Keys.OPERATION, "value");
			node.setProperty(Keys.OPERAND1, expr.toString());
		} else {
			node.setProperty(Keys.OPERATION, "Unknown condition value: " + condition.toString());
		}

		return node;
	}

	private String valueToString(final Node node, final Value value) {
		if (value instanceof Immediate) {
			Immediate expr = (Immediate) value;

			return expr.toString();
		} else if (value instanceof LengthExpr) {
			LengthExpr expr = (LengthExpr) value;

			return expr.getOp().toString();
		}

		return "Unhandled condition value part: " + value.toString();
	}

}
