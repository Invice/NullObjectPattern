package de.chw.sdg.soot.visitor;

public interface Visitor<P, R> {

	R visit(P parameter);
}
