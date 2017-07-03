package de.chw.sdg.soot.visitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.LoggerFactory;

import de.chw.sdg.db.RelTypes;
import de.chw.sdg.db.transformer.node.DefinitionTransformer;
import de.chw.sdg.db.transformer.node.FieldTransformer;
import de.chw.sdg.db.transformer.node.IfTransformer;
import de.chw.sdg.db.transformer.node.MethodCallTransformer;
import de.chw.sdg.db.transformer.node.MethodTransformer;
import de.chw.sdg.db.transformer.node.NopTransformer;
import de.chw.sdg.db.transformer.node.PackageTransformer;
import de.chw.sdg.db.transformer.node.ReturnTransformer;
import de.chw.sdg.db.transformer.node.ThrowStmtTransformer;
import de.chw.sdg.db.transformer.node.TypeTransformer;
import de.chw.sdg.db.transformer.node.UnitTransformer;
import de.chw.sdg.db.transformer.relationship.AggregatedFieldReadTransformer;
import de.chw.sdg.db.transformer.relationship.AggregatedFieldWriteTransformer;
import de.chw.sdg.db.transformer.relationship.ControlflowTransformer;
import de.chw.sdg.db.transformer.relationship.DataflowTransformer;
import de.chw.sdg.db.transformer.relationship.GenericRelationshipTransformer;
import de.chw.sdg.db.transformer.util.Aggregate;
import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.soot.traverser.PdgNavigator;
import soot.Body;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.NopStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JEndNopStmt;
import soot.tagkit.Host;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;

public class Neo4JVisitor implements StatementVisitor<Node> {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Neo4JVisitor.class);

	/**
	 * Used to resolve predecessor or successor Neo4J node of a given Soot host.
	 */
	private final VisitorMap<String, Node> packageNodes = new VisitorMap<>(new Visitor<String, Void>() {
		@Override
		public Void visit(final String parameter) {
			Neo4JVisitor.this.visit(parameter);
			return null;
		}
	});
	private final VisitorMap<SootClass, Node> typeNodes = new VisitorMap<>(new Visitor<SootClass, Void>() {
		@Override
		public Void visit(final SootClass parameter) {
			Neo4JVisitor.this.visit(parameter);
			return null;
		}
	});
	private final VisitorMap<SootMethod, Node> methodNodes = new VisitorMap<>(new Visitor<SootMethod, Void>() {
		@Override
		public Void visit(final SootMethod parameter) {
			Neo4JVisitor.this.visit(parameter);
			return null;
		}
	});
	private final VisitorMap<SootField, Node> fieldNodes = new VisitorMap<>(new Visitor<SootField, Void>() {
		@Override
		public Void visit(final SootField parameter) {
			Neo4JVisitor.this.visit(parameter);
			return null;
		}
	});
	private final Map<Unit, Node> unitNodes = new HashMap<>();

	private final GenericRelationshipTransformer containsUnitTransformer;
	private final ControlflowTransformer controlflowTransformer;
	private final DataflowTransformer dataflowTransformer;
	private final GenericRelationshipTransformer callsTransformer;

	private final GenericRelationshipTransformer aggregatedCallsTransformer;
	private final AggregatedFieldReadTransformer aggregatedFieldReadTransformer;
	private final AggregatedFieldWriteTransformer aggregatedFieldWriteTransformer;
	private final GenericRelationshipTransformer throwsTransformer;

	private final GenericRelationshipTransformer containsPackageTransformer;
	private final GenericRelationshipTransformer containsTypeTransformer;
	private final GenericRelationshipTransformer containsMethodTransformer;
	private final GenericRelationshipTransformer containsConstructorTransformer;
	private final GenericRelationshipTransformer containsFieldTransformer;
	private final GenericRelationshipTransformer lastUnitTransformer;
	private final GenericRelationshipTransformer callerOfTransformer;
	private final GenericRelationshipTransformer aggregatedCtrlFlowTransformer;

	private final UnitTransformer unitTransformer;
	private final IfTransformer ifTransformer;
	private final MethodCallTransformer invokeTransformer;
	private final DefinitionTransformer definitionTransformer;
	private final MethodTransformer methodTransformer;
	private final FieldTransformer fieldTransformer;
	private final TypeTransformer classTransformer;
	private final PackageTransformer packageTransformer;

	private final GenericRelationshipTransformer extendsTransformer;
	private final GenericRelationshipTransformer implementsTransformer;

	private final ReturnTransformer returnTransformer;
	private final ThrowStmtTransformer throwStmtTransformer;

	// private final Pattern EXTENDS_BY_ORIGIN_PATTERN;
	// private final Pattern IMPLEMENTS_BY_ORIGIN_PATTERN;

	private final NopTransformer nopTransformer;

	public Neo4JVisitor(final GraphDatabaseService graphDatabaseService) {
		containsUnitTransformer = new GenericRelationshipTransformer(RelTypes.CONTAINS_UNIT);
		controlflowTransformer = new ControlflowTransformer();
		dataflowTransformer = new DataflowTransformer();
		callsTransformer = new GenericRelationshipTransformer(RelTypes.CALLS);
		throwsTransformer = new GenericRelationshipTransformer(RelTypes.THROWS);

		aggregatedCallsTransformer = new GenericRelationshipTransformer(RelTypes.AGGREGATED_CALLS);
		aggregatedFieldReadTransformer = new AggregatedFieldReadTransformer();
		aggregatedFieldWriteTransformer = new AggregatedFieldWriteTransformer();
		aggregatedCtrlFlowTransformer = new GenericRelationshipTransformer(RelTypes.AGGR_CTRL_FLOW);

		extendsTransformer = new GenericRelationshipTransformer(RelTypes.EXTENDS);
		implementsTransformer = new GenericRelationshipTransformer(RelTypes.IMPLEMENTS);

		containsPackageTransformer = new GenericRelationshipTransformer(RelTypes.CONTAINS_PACKAGE);
		containsTypeTransformer = new GenericRelationshipTransformer(RelTypes.CONTAINS_TYPE);
		containsMethodTransformer = new GenericRelationshipTransformer(RelTypes.CONTAINS_METHOD);
		containsConstructorTransformer = new GenericRelationshipTransformer(RelTypes.CONTAINS_CONSTRUCTOR);
		containsFieldTransformer = new GenericRelationshipTransformer(RelTypes.CONTAINS_FIELD);
		lastUnitTransformer = new GenericRelationshipTransformer(RelTypes.LAST_UNIT);
		callerOfTransformer = new GenericRelationshipTransformer(RelTypes.CALLER_OF);

		unitTransformer = new UnitTransformer(graphDatabaseService);
		ifTransformer = new IfTransformer(graphDatabaseService);
		invokeTransformer = new MethodCallTransformer(graphDatabaseService);
		definitionTransformer = new DefinitionTransformer(graphDatabaseService, invokeTransformer);

		packageTransformer = new PackageTransformer(graphDatabaseService);
		classTransformer = new TypeTransformer(graphDatabaseService);
		methodTransformer = new MethodTransformer(graphDatabaseService);
		fieldTransformer = new FieldTransformer(graphDatabaseService);

		returnTransformer = new ReturnTransformer(graphDatabaseService);
		throwStmtTransformer = new ThrowStmtTransformer(graphDatabaseService);
		nopTransformer = new NopTransformer(graphDatabaseService);

		// EXTENDS_BY_ORIGIN_PATTERN =
		// Pattern.compile(FilterUtil.INSTANCE.getRegex("extends.by.origin"));
		// IMPLEMENTS_BY_ORIGIN_PATTERN =
		// Pattern.compile(FilterUtil.INSTANCE.getRegex("implements.by.origin"));
	}

	@Override
	public void visitFwdCtrlFlow(final Unit sourceUnit, final Unit targetUnit, final PdgNavigator pdgNavigator) {
		Node sourceNode = unitNodes.get(sourceUnit);
		Node targetNode = unitNodes.get(targetUnit);

		transformContainsUnit(sourceNode, sourceUnit, targetNode);
		controlflowTransformer.transform(sourceUnit, targetUnit, null, sourceNode, targetNode);
	}

	private void transformContainsUnit(final Node sourceNode, final Unit sourceUnit, final Node targetNode) {
		if (sourceNode == null) {
			LOGGER.error("null: {}, class = {}", sourceUnit, sourceUnit.getClass());
		} else if (sourceUnit instanceof NopStmt) { // hierarchy starts here
			if (sourceNode.hasProperty(Keys.NOP_KIND)) {
				String nopKind = (String) sourceNode.getProperty(Keys.NOP_KIND);
				switch (nopKind) {
				case "FOR_INIT":
				case "FOREACH_INIT":
				case "WHILE_COND":
				case "DO_WHILE_BODY":
				case "TRY_BEGIN":
				case "SWITCH_COND":
				case "IF_COND":
					// bodies (requires BODY_END nops; not yet implemented
					// 15.06.16)
					// case "FOR_BODY":
					// case "FOREACH_BODY":
					// case "WHILE_BODY":
					containsUnitTransformer.transform(sourceNode, targetNode);
					break;
				default:
					// ignore all other nop stmts
				}
			} else {
				LOGGER.warn("Found NopStmt without nopKind: {}", sourceNode);
			}
		}
	}

	@Override
	public void visitFwdCtrlFlow(final SootMethod method, final Host targetUnit) {
		Node sourceNode = methodNodes.get(method);
		Node targetNode = unitNodes.get(targetUnit);

		controlflowTransformer.transform(method, targetUnit, null, sourceNode, targetNode);
		containsUnitTransformer.transform(sourceNode, targetNode);
	}

	@Override
	public void visitFwdDataFlow(final Host sourceUnit, final Host targetUnit, final ValueBox valueBox) {
		Node sourceNode = unitNodes.get(sourceUnit);
		Node targetNode = unitNodes.get(targetUnit);

		if (sourceNode == null) {
			LOGGER.error("sourceNode is null: " + sourceUnit);
		}

		if (targetNode == null) {
			LOGGER.error("targetNode is null: " + targetUnit + ", class: " + targetUnit.getClass());
			return; // FIXME workaround: just ignore this dataflow edge for now
		}

		dataflowTransformer.transform(sourceUnit, targetUnit, valueBox, sourceNode, targetNode);

		if (targetNode.hasLabel(MethodCallTransformer.TYPE_WITH_RETURN_VALUE_LABEL)) {
			String caller = (String) targetNode.getProperty(Keys.CALLER);
			if (caller.equals(valueBox.getValue().toString())) {
				callerOfTransformer.transform(sourceNode, targetNode);
			}
		}
	}

	@Override
	public Node visit(final DefinitionStmt unit, final UnitGraph unitGraph) {
		Node node = definitionTransformer.transform(unit, unitGraph.getBody().getMethod());

		if (unitNodes.containsKey(unit)) {
			LOGGER.error("duplicate unit: " + unit);
		}
		unitNodes.put(unit, node);

		return node;
	}

	@Override
	public Node visit(final IfStmt unit, final UnitGraph unitGraph) {
		Node node = ifTransformer.transform(unit);
		unitNodes.put(unit, node);
		return node;
	}

	@Override
	public Node visit(final InvokeStmt unit, final UnitGraph unitGraph) {
		Node node = invokeTransformer.transform(unit.getInvokeExpr(), unitGraph.getBody().getMethod());
		unitNodes.put(unit, node);
		return node;
	}

	@Override
	public Node visit(final Unit unit, final UnitGraph unitGraph) {
		Node node = unitTransformer.transform(unit);
		unitNodes.put(unit, node);

		return node;
	}

	public void executeInterProceduralOperations() {
		LOGGER.info("Transforming inter procedural connections...");

		for (Entry<SootMethod, Node> staticCall : invokeTransformer.getInvocations().entries()) {
			Node sourceNode = staticCall.getValue();

			SootMethod targetMethod = staticCall.getKey();
			Node targetNode = methodNodes.getValue(targetMethod);

			callsTransformer.transform(sourceNode, targetNode);
		}

		for (Entry<SootMethod, SootMethod> staticCall : invokeTransformer.getAggregatedInvocations().entries()) {
			SootMethod sourceMethod = staticCall.getKey();
			SootMethod targetMethod = staticCall.getValue();

			aggregatedCallsTransformer.transform(methodNodes.get(sourceMethod), methodNodes.get(targetMethod));
		}

		for (Pair<SootField, DefinitionStmt> fieldAccess : definitionTransformer.getFieldReadAccesses()) {
			final SootField field = fieldAccess.getO1();
			final DefinitionStmt unit = fieldAccess.getO2();

			Node sourceNode = fieldNodes.getValue(field);
			Node targetNode = unitNodes.get(unit);

			dataflowTransformer.transform(null, null, unit.getRightOpBox(), sourceNode, targetNode);
		}

		for (Pair<DefinitionStmt, SootField> fieldAccess : definitionTransformer.getFieldWriteAccesses()) {
			final DefinitionStmt unit = fieldAccess.getO1();
			final SootField field = fieldAccess.getO2();

			Node sourceNode = unitNodes.get(unit);
			Node targetNode = fieldNodes.getValue(field);

			dataflowTransformer.transform(null, null, unit.getRightOpBox(), sourceNode, targetNode);
		}

		for (Aggregate aggregate : definitionTransformer.getAggregatedFieldReads().values()) {
			Node sourceNode = methodNodes.get(aggregate.getMethod());
			Node targetNode = fieldNodes.getValue(aggregate.getField());

			aggregatedFieldReadTransformer.transform(sourceNode, targetNode, aggregate.amount);
		}

		for (Aggregate aggregate : definitionTransformer.getAggregatedFieldWrites().values()) {
			Node sourceNode = methodNodes.get(aggregate.getMethod());
			Node targetNode = fieldNodes.getValue(aggregate.getField());

			aggregatedFieldWriteTransformer.transform(sourceNode, targetNode, aggregate.amount);
		}

		LOGGER.info("Finished: Transforming inter procedural connections.");
	}

	@Override
	public void visit(final String packageName) {
		if (packageNodes.containsKey(packageName)) {
			return;
		}
		Node node = packageTransformer.transform(packageName);
		packageNodes.put(packageName, node);
	}

	@Override
	public void visit(final SootClass clazz) {
		if (typeNodes.containsKey(clazz)) {
			return;
		}

		Node node = classTransformer.transform(clazz);
		typeNodes.put(clazz, node);

		String packageName = clazz.getPackageName();
		Node packageNode = packageNodes.getValue(packageName);
		containsTypeTransformer.transform(packageNode, node);

		if (clazz.isInnerClass()) {
			Node outerClassNode = typeNodes.getValue(clazz.getOuterClass()); // inner
			// can
			// be
			// visited
			// before
			// outer
			containsTypeTransformer.transform(outerClassNode, node);
		}

		if (!clazz.isApplicationClass()) {// limit the construction of lib and
			// java types
			return;
		}

		for (SootClass interface_ : clazz.getInterfaces()) {
			Node interfaceNode = typeNodes.getValue(interface_);

			// String origin = (String)
			// interfaceNode.getProperty(TypeTransformer.KORIGIN);
			// if (IMPLEMENTS_BY_ORIGIN_PATTERN.matcher(origin).matches()) {
			implementsTransformer.transform(node, interfaceNode);
			// }
		}

		if (clazz.hasSuperclass()) {
			SootClass superclass = clazz.getSuperclass();
			Node superClassNode = typeNodes.getValue(superclass);

			// String origin = (String)
			// superClassNode.getProperty(TypeTransformer.KORIGIN);
			// if (EXTENDS_BY_ORIGIN_PATTERN.matcher(origin).matches()) {
			extendsTransformer.transform(node, superClassNode);
			// }
		}

	}

	@Override
	public void visit(final SootMethod method) {
		// precondition: the method's type has already been created
		Node node = methodTransformer.transform(method);
		methodNodes.put(method, node);

		Node declaringClassNode = typeNodes.getValue(method.getDeclaringClass());
		if (method.isConstructor()) {
			containsConstructorTransformer.transform(declaringClassNode, node);
		} else {
			containsMethodTransformer.transform(declaringClassNode, node);
		}

		// throws header
		for (SootClass exception : method.getExceptions()) {
			Node exceptionDeclNode = typeNodes.getValue(exception);
			throwsTransformer.transform(node, exceptionDeclNode);
		}
	}

	@Override
	public void visit(final SootField field) {
		LOGGER.debug("Field: " + field);
		Node node = fieldTransformer.transform(field);
		Node previousNode = fieldNodes.put(field, node);
		if (previousNode != null) {
			LOGGER.error("Field has already been declared: " + field + ", node: " + node);
		}

		Node declaringClassNode = typeNodes.getValue(field.getDeclaringClass());
		containsFieldTransformer.transform(declaringClassNode, node);
	}

	@Override
	public Node visit(final ReturnStmt unit, final UnitGraph unitGraph) {
		Node node = returnTransformer.transform(unit);
		unitNodes.put(unit, node);
		return node;
	}

	@Override
	public Node visit(final ReturnVoidStmt unit, final UnitGraph unitGraph) {
		Node node = returnTransformer.transform(unit);
		unitNodes.put(unit, node);
		return node;
	}

	@Override
	public Node visit(final NopStmt unit, final UnitGraph unitGraph) {
		Node node = nopTransformer.transform(unit);
		unitNodes.put(unit, node);

		return node;
	}

	@Override
	public Node visit(final ThrowStmt unit, final UnitGraph unitGraph) {
		Node node = throwStmtTransformer.transform(unit);
		unitNodes.put(unit, node);
		return node;
	}

	@Override
	public void visitVisitedUnit(final Unit unit, final PdgNavigator pdgNavigator) {
		Node targetNode = unitNodes.get(unit);

		if (unit instanceof JEndNopStmt) {
			// String nopKind = (String)
			// targetNode.getProperty(NopTransformer.KNOP_KIND);
			// switch (nopKind) {
			// case "IF_END":
			// case "FOR_END":
			// case "FOREACH_END":
			// case "SWITCH_END":
			Stmt beginCondStmt = ((JEndNopStmt) unit).getBeginCond();
			if (beginCondStmt != null) {
				Node beginCondNode = unitNodes.get(beginCondStmt);
				lastUnitTransformer.transform(targetNode, beginCondNode);

				Unit successor = pdgNavigator.getSuccessorOf(unit);
				Node successorNode = unitNodes.get(successor);
				aggregatedCtrlFlowTransformer.transform(beginCondNode, successorNode);
			} else {
				// do nothing
				// e.g., handles the break stmt and the false case of an if stmt without a false branch
			}
		} else if (unit instanceof ReturnStmt
				|| unit instanceof ReturnVoidStmt
				|| unit instanceof ThrowStmt) {
			// BodyTag bodyTag = (BodyTag) targetUnit.getTag(BodyTag.NAME);
			// if (bodyTag == null) {
			// LOGGER.error("bodytag is null: " + targetUnit + ", class = " +
			// targetUnit.getClass());
			// }
			// Body body = bodyTag.getBody();
			Body body = pdgNavigator.getBody();
			SootMethod methodOfReturn = body.getMethod();
			Node methodNode = methodNodes.get(methodOfReturn);
			lastUnitTransformer.transform(targetNode, methodNode);
		}
	}

}
