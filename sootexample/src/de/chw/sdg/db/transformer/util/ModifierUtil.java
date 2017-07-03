package de.chw.sdg.db.transformer.util;

import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.node.property.Visibility;
import soot.Modifier;

public class ModifierUtil {

	private static final String KFINAL = "isfinal";
	private static final String KVISIBILITY = "visibility";
	private static final String KABSTARCT = "isabstract";
	private static final String KSTATIC = "isstatic";
	private static final String KNATIVE = "isnative";
	private static final String KSYNTHETIC = "issynthetic";
	// for fields
	private static final String KVOLATILE = "isvolatile";
	private static final String KTRANSIENT = "istransient";

	private ModifierUtil() {
		// utility class
	}

	public static void setModifiers(final Node node, final int modifiers) {
		node.setProperty(KABSTARCT, Modifier.isAbstract(modifiers));
		node.setProperty(KFINAL, Modifier.isFinal(modifiers));
		node.setProperty(KNATIVE, Modifier.isNative(modifiers));
		node.setProperty(KVISIBILITY, Visibility.resolve(modifiers));
		node.setProperty(KSTATIC, Modifier.isStatic(modifiers));
		if (Modifier.isStrictFP(modifiers)) {

		}
		if (Modifier.isSynchronized(modifiers)) {

		}
		if (Modifier.isSynthetic(modifiers)) {
			node.setProperty(KSYNTHETIC, Modifier.isSynthetic(modifiers));
		}
		if (Modifier.isVolatile(modifiers)) {
			node.setProperty(KVOLATILE, Modifier.isVolatile(modifiers));
		}
		if (Modifier.isTransient(modifiers)) {
			node.setProperty(KTRANSIENT, Modifier.isTransient(modifiers));
		}
		if (Modifier.isAnnotation(modifiers)) {

		}
		if (Modifier.isDeclaredSynchronized(modifiers)) {

		}

		// if (Modifier.isConstructor(modifiers)) {
		//
		// }
		// if (Modifier.isEnum(modifiers)) {
		//
		// }
		// if (Modifier.isInterface(modifiers)) {
		//
		// }
	}
}
