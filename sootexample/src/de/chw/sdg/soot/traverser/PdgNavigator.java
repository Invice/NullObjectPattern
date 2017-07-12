package de.chw.sdg.soot.traverser;

import soot.Body;
import soot.Unit;

public interface PdgNavigator {

	Body getBody();

	/**
	 * 
	 * @param unit
	 * @return	the only successor of the given <code>unit</code> or <code>null</code> otherwise (e.g., in case of previous return stmts both in then and else branch).
	 */
	Unit getSuccessorOf(Unit unit);
}
