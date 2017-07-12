package de.chw.sdg.soot.traverser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.slf4j.LoggerFactory;

import de.chw.sdg.soot.visitor.StatementVisitor;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.NopStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ThrowStmt;
import soot.toolkits.graph.UnitGraph;

public class UnitGraphTraverser implements PdgNavigator {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UnitGraphTraverser.class);

	private static boolean skipGoto = Boolean.getBoolean("skipGoto");

	private final Set<Unit> visitedUnits = new HashSet<>();

	private final UnitGraph unitGraph;
	// private final RegionAnalysis regionAnalysis;

	public UnitGraphTraverser(final UnitGraph unitGraph) {
		this.unitGraph = unitGraph;
		// this.regionAnalysis = new RegionAnalysis(unitGraph,
		// unitGraph.getBody().getMethod(),
		// unitGraph.getBody().getMethod().getDeclaringClass());
		// for (Region region : regionAnalysis.getRegions()) {
		// System.out.println("region: " + region);
		// }

		// BriefBlockGraph blockGraph = new BriefBlockGraph(cfg);
		// blockGraph.ge
		// for (Block block : blockGraph.getHeads()) {
		// System.out.println("block: "+block);
		// }
		//
		// for (Block block : blockGraph.getTails()) {
		// System.out.println("tails: "+block);
		// }
	}

	public void traverse(final StatementVisitor<Node> visitor, final SootMethod sootMethod) {
		// transform units and control flow relationships
		List<Unit> heads = unitGraph.getHeads();

		if (heads.size() > 1) {
			LOGGER.info("Multiple heads in method: " + sootMethod.getSignature());
			for (Unit unit : heads) {
				LOGGER.debug("head: " + unit);
			}
		}

		for (Unit entryUnit : heads) {
			traverse(entryUnit, visitor);
			visitor.visitFwdCtrlFlow(sootMethod, entryUnit);
		}
	}

	private void traverse(final Unit unit, final StatementVisitor<Node> visitor) {
		if (!visitedUnits.add(unit)) {
			return;
		}

		@SuppressWarnings("unused")
		Node node;
		if (unit instanceof DefinitionStmt) {
			DefinitionStmt defStmt = (DefinitionStmt) unit;
			node = visitor.visit(defStmt, unitGraph);
		} else if (unit instanceof InvokeStmt) {
			node = visitor.visit((InvokeStmt) unit, unitGraph);
		} else if (unit instanceof IfStmt) {
			node = visitor.visit((IfStmt) unit, unitGraph);
		} else if (unit instanceof ReturnStmt) {
			node = visitor.visit((ReturnStmt) unit, unitGraph);
		} else if (unit instanceof ReturnVoidStmt) {
			node = visitor.visit((ReturnVoidStmt) unit, unitGraph);
		} else if (unit instanceof ThrowStmt) {
			node = visitor.visit((ThrowStmt) unit, unitGraph);
		} else if (unit instanceof NopStmt) {
			node = visitor.visit((NopStmt) unit, unitGraph);
		} else {
			node = visitor.visit(unit, unitGraph);
		}

		List<Unit> successorUnits = getSuccessorUnits(unit);
		for (Unit successorUnit : successorUnits) {
			successorUnit = skip(successorUnit);

			traverse(successorUnit, visitor);
			visitor.visitFwdCtrlFlow(unit, successorUnit, this);
		}
	}

	private Unit skip(Unit successorUnit) {
		while (skipGoto && successorUnit instanceof GotoStmt) { // skip gotos
			successorUnit = ((GotoStmt) successorUnit).getTarget();
		}

		return successorUnit;
	}

	private List<Unit> getSuccessorUnits(final Unit unit) {
		return unitGraph.getSuccsOf(unit);
	}

	public Set<Unit> getVisitedUnits() {
		return visitedUnits;
	}

	@Override
	public Body getBody() {
		return unitGraph.getBody();
	}

	@Override
	public Unit getSuccessorOf(Unit unit) {
		List<Unit> successorUnits = getSuccessorUnits(unit);
		if (successorUnits.size() == 0) {
			return null;
		} else if (successorUnits.size() > 1) {
			throw new IllegalStateException("Unit has " + successorUnits.size() + " successors. It should have exactly 1.");
		}
		
		Unit successor = successorUnits.get(0);
		successor = skip(successor);
		return successor;
	}

}
