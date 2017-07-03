package de.chw.sdg.db.transformer.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Aggregate;
import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.Immediate;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.JastAddJ.Access;
import soot.javaToJimple.jj.extension.FinalTag;
import soot.javaToJimple.jj.extension.SyntheticLocalVarTag;
import soot.javaToJimple.jj.extension.TypeArgumentsTag;
import soot.javaToJimple.jj.extension.VariableDeclarationTag;
import soot.jimple.ArrayRef;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceOfExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.LengthExpr;
import soot.jimple.NegExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.tagkit.LineNumberTag;
import soot.toolkits.scalar.Pair;

public class DefinitionTransformer extends AbstractNodeTransformer {

	private static final String LINE_NUMBER_TAG_NAME = new LineNumberTag(0).getName();
	private static final String VARIABLE_DECLARATION_NAME = VariableDeclarationTag.INSTANCE.getName();

	// labels
	private static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("definition");

	private final MethodCallTransformer methodCallTransformer;
	private final NewArrayTransformer newArrayTransformer;

	private final List<Pair<SootField, DefinitionStmt>> fieldReadAccesses = new ArrayList<>();
	private final List<Pair<DefinitionStmt, SootField>> fieldWriteAccesses = new ArrayList<>();

	private final Map<String, Aggregate> aggregatedFieldReads = new HashMap<>();
	private final Map<String, Aggregate> aggregatedFieldWrites = new HashMap<>();

	public DefinitionTransformer(final GraphDatabaseService graphDatabaseService,
			final MethodCallTransformer invokeTransformer) {
		super(graphDatabaseService);
		this.methodCallTransformer = invokeTransformer;
		this.newArrayTransformer = new NewArrayTransformer(graphDatabaseService);
	}

	// a definition statement is either an assignment (AssignStmt) or an identity statement (IdentityStmt)
	public Node transform(final DefinitionStmt unit, final SootMethod method) {
		Node node = createNodeWithDisplayName(unit, method);

		// Value rightOp = unit.getRightOp();
		Value leftOp = unit.getLeftOp();

		if (unit.hasTag(LINE_NUMBER_TAG_NAME)) {
			LineNumberTag lineNumberTag = (LineNumberTag) unit.getTag(LINE_NUMBER_TAG_NAME);
			int lineNumber = lineNumberTag.getLineNumber();
			node.setProperty(Keys.LINE_NUMBER, lineNumber);
		}

		if (unit.hasTag(VARIABLE_DECLARATION_NAME)) {
			node.setProperty(Keys.IS_DECLARATION, Boolean.TRUE);
		}

		// custom properties
		processOperation(method, node, unit);

		String varName;
		// write access: single and aggregation
		if (leftOp instanceof FieldRef) {
			FieldRef leftOperation = (FieldRef) leftOp;
			SootField field = leftOperation.getField();

			fieldWriteAccesses.add(new Pair<>(unit, field));

			final String key = method.getSignature() + field.getSignature();

			if (!aggregatedFieldWrites.containsKey(key)) {
				Aggregate aggregate = new Aggregate(method, field);
				aggregatedFieldWrites.put(key, aggregate);
			}
			Aggregate aggregate = aggregatedFieldWrites.get(key);
			aggregate.amount++;

			varName = field.getName();
		} else {
			varName = leftOp.toString();
		}

		node.setProperty(Keys.VAR, varName);
		node.setProperty(Keys.VAR_TYPE, leftOp.getType().toString());

		if (unit.getLeftOpBox().hasTag(TypeArgumentsTag.NAME)) {
			TypeArgumentsTag typeParametersTag = (TypeArgumentsTag) unit.getLeftOpBox().getTag(TypeArgumentsTag.NAME);
			List<Access> tagList = typeParametersTag.getTypeArguments();
			String[] lefttypeargs = new String[tagList.size()];
			for (int i = 0; i < tagList.size(); i++) {
				Access a = tagList.get(i);
				lefttypeargs[i] = a.toString();
			}
			node.setProperty(Keys.LEFT_TYPE_ARGS, lefttypeargs);
		}

		if (unit.hasTag(FinalTag.NAME)) {
			node.setProperty(Keys.IS_FINAL, Boolean.TRUE);
		}

		if (unit.hasTag(SyntheticLocalVarTag.NAME)) {
			node.setProperty(Keys.IS_SYNTHETIC, Boolean.TRUE);
		}

		return node;
	}

	private void processOperation(final SootMethod method, final Node node, final DefinitionStmt unit) {
		final Value rightOp = unit.getRightOp();
		String rightHandSideValue = rightOp.toString(); // default

		if (rightOp instanceof BinopExpr) {
			// TODO add left and right value to the data flow edges
			BinopExpr expr = (BinopExpr) rightOp;
			node.setProperty(Keys.OPERATION, expr.getSymbol().trim());

			node.setProperty(Keys.OPERAND1, expr.getOp1().toString());
			node.setProperty(Keys.OPERAND2, expr.getOp2().toString());
		} else if (rightOp instanceof LengthExpr) {
			LengthExpr expr = (LengthExpr) rightOp;
			rightHandSideValue = expr.getOp().toString();
			node.setProperty(Keys.OPERATION, "length");
		} else if (rightOp instanceof NegExpr) {
			NegExpr expr = (NegExpr) rightOp;
			rightHandSideValue = expr.getOp().toString();
			node.setProperty(Keys.OPERATION, "not");
		} else if (rightOp instanceof InvokeExpr) {
			node.setProperty(Keys.OPERATION, MethodCallTransformer.TYPE_WITH_RETURN_VALUE_LABEL.name());
		} else if (rightOp instanceof Immediate) { // e.g., 1234
			node.setProperty(Keys.OPERATION, "value");
		} else if (rightOp instanceof InstanceFieldRef) { // must precede FieldRef
			InstanceFieldRef fieldRef = (InstanceFieldRef) rightOp;
			SootField field = fieldRef.getField();
			fieldReadAccesses.add(new Pair<>(field, unit));
			addAggregateField(method, field);
			rightHandSideValue = fieldRef.getBase().toString() + "." + field.getName();
			node.setProperty(Keys.OPERATION, "instanceFieldAccess");
		} else if (rightOp instanceof FieldRef) { // static field
			FieldRef fieldRef = (FieldRef) rightOp;
			SootField field = fieldRef.getField();
			fieldReadAccesses.add(new Pair<>(field, unit));
			addAggregateField(method, field);
			rightHandSideValue = field.getDeclaringClass().toString() + "." + field.getName();
			node.setProperty(Keys.OPERATION, "staticFieldAccess");
		} else if (rightOp instanceof ArrayRef) {
			ArrayRef expr = (ArrayRef) rightOp;
			node.setProperty(Keys.INDEX, expr.getIndex().toString());
			node.setProperty(Keys.BASE, expr.getBase().toString());
			node.setProperty(Keys.OPERATION, "arrayaccess");
		} else if (rightOp instanceof CaughtExceptionRef) {
			node.setProperty(Keys.OPERATION, "caughtexception");
		} else if (rightOp instanceof ParameterRef) {
			node.setProperty(Keys.OPERATION, "parameterdeclaration");
		} else if (rightOp instanceof ThisRef) {
			node.setProperty(Keys.OPERATION, "thisdeclaration");
		} else if (rightOp instanceof CastExpr) {
			CastExpr expr = (CastExpr) rightOp;
			rightHandSideValue = expr.getOp().toString();
			node.setProperty(Keys.OPERATION, "cast");
		} else if (rightOp instanceof InstanceOfExpr) {
			InstanceOfExpr expr = (InstanceOfExpr) rightOp;
			rightHandSideValue = expr.getOp().toString();
			node.setProperty(Keys.OPERATION, "instanceof");
		} else if (rightOp instanceof NewExpr) {
//			NewExpr expr = (NewExpr) rightOp;
//			RefType baseType = expr.getBaseType();
			// SootClass clazz = baseType.getSootClass();
			node.setProperty(Keys.OPERATION, "new");
		} else if (rightOp instanceof NewArrayExpr) {
			NewArrayExpr expr = (NewArrayExpr) rightOp;
			newArrayTransformer.transform(node, expr);
			rightHandSideValue = "new " + node.getProperty(Keys.BASE_TYPE) + "[" + node.getProperty(Keys.SIZE) + "]";
			// PhiExpr is only used within the pack "stp/sop"
		} else {
			// do nothing
			String operation = "unknown: " + rightOp + " (" + rightOp.getClass() + ")";
			node.setProperty(Keys.OPERATION, operation);
		}

		node.setProperty(Keys.RIGHT_HAND_SIDE_VALUE, rightHandSideValue);
	}

	private Node createNodeWithDisplayName(final DefinitionStmt unit, final SootMethod method) {
		Value leftOp = unit.getLeftOp();
		Value rightOp = unit.getRightOp();

		Node node = createNode();

		String rightHandSide;
		if (rightOp instanceof InvokeExpr) {
			rightHandSide = methodCallTransformer.transform(node, (InvokeExpr) rightOp, method);
			// type and label is set by the transformer
		} else {
			rightHandSide = rightOp.toString();
			node.addLabel(TYPE_LABEL);
			node.setProperty(Keys.TYPE, TYPE_LABEL.name());
		}

		node.setProperty(Keys.DISPLAYNAME, leftOp + " = " + rightHandSide);

		return node;
	}

	private void addAggregateField(final SootMethod method, final SootField field) {
		final String key = method.getSignature() + field.getSignature();

		if (!aggregatedFieldReads.containsKey(key)) {
			Aggregate aggregate = new Aggregate(method, field);
			aggregatedFieldReads.put(key, aggregate);
		}
		Aggregate aggregate = aggregatedFieldReads.get(key);
		aggregate.amount++;
	}

	public List<Pair<SootField, DefinitionStmt>> getFieldReadAccesses() {
		return fieldReadAccesses;
	}

	public List<Pair<DefinitionStmt, SootField>> getFieldWriteAccesses() {
		return fieldWriteAccesses;
	}

	public Map<String, Aggregate> getAggregatedFieldReads() {
		return aggregatedFieldReads;
	}

	public Map<String, Aggregate> getAggregatedFieldWrites() {
		return aggregatedFieldWrites;
	}

}
