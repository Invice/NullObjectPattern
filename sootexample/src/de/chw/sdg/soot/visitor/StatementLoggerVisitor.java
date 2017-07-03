package de.chw.sdg.soot.visitor;

import org.neo4j.graphdb.Node;

import de.chw.sdg.soot.traverser.PdgNavigator;
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
import soot.jimple.ThrowStmt;
import soot.tagkit.Host;
import soot.toolkits.graph.UnitGraph;

public class StatementLoggerVisitor implements StatementVisitor<Node> {

	// private static final org.slf4j.Logger LOGGER =
	// LoggerFactory.getLogger(StatementLoggerVisitor.class);

	@Override
	public Node visit(final IfStmt stmt, final UnitGraph unitGraph) {
		System.out.println("if: " + stmt + ", branches: " + stmt.branches() + ", fallsThrough: " + stmt.fallsThrough());
		System.out.println("condition: " + stmt.getCondition() + ", target: " + stmt.getTarget());
		System.out.println("pointing to this: " + stmt.getBoxesPointingToThis());

		System.out.println("unit boxes: " + stmt.getUnitBoxes());
		System.out.println("successors: " + unitGraph.getSuccsOf(stmt));
		return null;
	}

	@Override
	public Node visit(final DefinitionStmt stmt, final UnitGraph unitGraph) {
		System.out.println("DefinitionStmt: " + stmt + ", " + stmt.getClass());
		// System.out.println("operation: " + stmt.getInvokeExpr()); // not
		// available if op is +-*/
		// System.out.println("operation box: " + stmt.getInvokeExprBox());//
		// not available if op is +-*/
		System.out.println("left op: " + stmt.getLeftOp());
		System.out.println("right op: " + stmt.getRightOp() + ", " + stmt.getRightOp().getClass());

		System.out.println("right op type: " + stmt.getRightOp().getType()); // ->
		// JAddExpr
		return null;
	}

	@Override
	public Node visit(final InvokeStmt stmt, final UnitGraph unitGraph) {
		System.out.println("InvokeStmt: " + stmt + ", " + stmt.getClass());
		return null;
	}

	@Override
	public Node visit(final Unit unit, final UnitGraph unitGraph) {
		System.out.println("unit: " + unit + ", " + unit.getClass());
		return null;
	}

	@Override
	public void visitFwdCtrlFlow(final Unit sourceUnit, final Unit targetUnit, final PdgNavigator pdgNavigator) {
		System.out.println("CF: " + sourceUnit + " -[control]-> " + targetUnit);
	}

	@Override
	public void visitFwdCtrlFlow(final SootMethod method, final Host targetUnit) {
		System.out.println("CF: " + method + " -[control]-> " + targetUnit);
	}

	@Override
	public void visitFwdDataFlow(final Host sourceUnit, final Host targetUnit, final ValueBox valueBox) {
		System.out.println("DF (" + valueBox + ") : " + sourceUnit + " -[data]-> " + targetUnit);
	}

	@Override
	public void visit(final SootMethod method) {
		System.out.println("MethodDecl: " + method);
	}

	@Override
	public void visit(final SootClass clazz) {
		System.out.println("ClassDecl: " + clazz);
	}

	@Override
	public void visit(final String packageName) {
		System.out.println("PackageDecl: " + packageName);
	}

	@Override
	public void visit(final SootField field) {
		System.out.println("FieldDecl: " + field);
	}

	@Override
	public Node visit(final ReturnStmt unit, final UnitGraph unitGraph) {
		System.out.println("ReturnStmt: " + unit + ", " + unit.getClass());
		return null;
	}

	@Override
	public Node visit(final ReturnVoidStmt unit, final UnitGraph unitGraph) {
		System.out.println("ReturnVoidStmt: " + unit + ", " + unit.getClass());
		return null;
	}

	@Override
	public Node visit(final NopStmt unit, final UnitGraph unitGraph) {
		System.out.println("NopStmt: " + unit + ", " + unit.getClass());
		return null;
	}

	@Override
	public Node visit(ThrowStmt unit, UnitGraph unitGraph) {
		System.out.println("ThrowStmt: " + unit + ", " + unit.getClass());
		return null;
	}

	@Override
	public void visitVisitedUnit(Unit unit, PdgNavigator pdgNavigator) {
		System.out.println("visitVisitedUnit: " + unit + ", " + unit.getClass());
	}

}
