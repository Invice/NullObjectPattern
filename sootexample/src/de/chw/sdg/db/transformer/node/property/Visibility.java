package de.chw.sdg.db.transformer.node.property;

import soot.Modifier;

public class Visibility {

	private Visibility() {
		// utility class
	}

	public static String resolve(final int modifiers) {
		if (Modifier.isPrivate(modifiers)) {
			return "private";
		}
		if (Modifier.isProtected(modifiers)) {
			return "protected";
		}
		if (Modifier.isPublic(modifiers)) {
			return "public";
		}
		return ""; // package-private
	}
}
