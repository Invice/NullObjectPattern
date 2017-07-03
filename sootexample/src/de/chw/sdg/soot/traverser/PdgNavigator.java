package de.chw.sdg.soot.traverser;

import soot.Body;
import soot.Unit;

public interface PdgNavigator {

	Body getBody();

	Unit getSuccessorOf(Unit unit);
}
