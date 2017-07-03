package de.chw.sdg.soot.traverser;

import java.util.List;

import org.neo4j.graphdb.Node;

import de.chw.sdg.soot.visitor.StatementVisitor;
import soot.Body;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.UnitValueBoxPair;

public class MethodBodyTraverser {

	//	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MethodBodyTraverser.class);

	public static final MethodBodyTraverser INSTANCE = new MethodBodyTraverser();

	private MethodBodyTraverser() {
		// singleton
	}

	public void traverse(final Body body, final StatementVisitor<Node> visitor) {
		visitor.visit(body.getMethod());

		// BlockGraph, UnitGraph. either Exceptional or Brief
		UnitGraph cfg = new BriefUnitGraph(body);
		// UnitGraph cfg = new ExceptionalUnitGraph(body);

		UnitGraphTraverser traverser = new UnitGraphTraverser(cfg);
		traverser.traverse(visitor, body.getMethod());

		SimpleLocalDefs simpleLocalDefs = new SimpleLocalDefs(cfg);
		SimpleLocalUses simpleLocalUses = new SimpleLocalUses(cfg, simpleLocalDefs);

		// invariant: at this point, all units of this unit graph were visited

		// transform data flow relationships
		for (Unit unit : traverser.getVisitedUnits()) {
			visitor.visitVisitedUnit(unit, traverser);

			List<UnitValueBoxPair> usingUnits = simpleLocalUses.getUsesOf(unit);
			for (UnitValueBoxPair pair : usingUnits) {
				Unit sourceUnit = unit;
				Unit targetUnit = pair.getUnit();

				visitor.visitFwdDataFlow(sourceUnit, targetUnit, pair.getValueBox());
			}
		}
	}
}
