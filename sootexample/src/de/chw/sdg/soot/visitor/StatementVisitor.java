package de.chw.sdg.soot.visitor;

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

public interface StatementVisitor<R>  {

	// R visit(TableSwitchStmt unit, UnitGraph unitGraph);

	R visit(DefinitionStmt unit, UnitGraph unitGraph);

	R visit(IfStmt unit, UnitGraph unitGraph);

	R visit(InvokeStmt unit, UnitGraph unitGraph);

	// R visit(GotoStmt unit, UnitGraph unitGraph);

	R visit(ThrowStmt unit, UnitGraph unitGraph);

	R visit(Unit unit, UnitGraph unitGraph);

	R visit(NopStmt unit, UnitGraph unitGraph);

	R visit(ReturnStmt unit, UnitGraph unitGraph);

	R visit(ReturnVoidStmt unit, UnitGraph unitGraph);

	void visitFwdCtrlFlow(Unit sourceUnit, Unit targetUnit, PdgNavigator pdgNavigator);

	void visitFwdCtrlFlow(SootMethod sourceUnit, Host targetUnit);

	void visitFwdDataFlow(Host sourceUnit, Host targetUnit, ValueBox valueBox);

	void visit(SootMethod method);

	void visit(SootClass clazz);

	void visit(String packageName);

	void visit(SootField field);

	void visitVisitedUnit(Unit unit, PdgNavigator pdgNavigator);

}
