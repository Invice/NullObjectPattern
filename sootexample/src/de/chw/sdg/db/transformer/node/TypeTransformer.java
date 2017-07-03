package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.node.property.Visibility;
import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.SootClass;
import soot.javaToJimple.jj.extension.TypeParametersTag;

public class TypeTransformer extends AbstractNodeTransformer {

	public static enum Origin {
		APP, LIB, JAVA, UNKNOWN;

		public static Origin valueOf(final int value) {
			switch (value) {
			case 0:
				return APP;
			case 1:
				return LIB;
			case 2:
				return JAVA;
			case 3:
				return UNKNOWN;
			default:
				throw new IllegalStateException("Unknown origin value: " + value);
			}
		}

		//		@Override
		//		public String toString() {
		//			return Integer.toString(value);
		//		}
	}

	private static final Label CLASS_LABEL = NamingUtil.INSTANCE.getLabel("classdeclaration");
	private static final Label INTERFACE_LABEL = NamingUtil.INSTANCE.getLabel("interfacedeclaration");

	public TypeTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final SootClass unit) {
		final Label label = (unit.isInterface()) ? INTERFACE_LABEL : CLASS_LABEL;
		Node node = createNode(label);

		node.setProperty(Keys.DISPLAYNAME, unit.getShortName());
		node.setProperty(Keys.NAME, unit.getShortName());
		node.setProperty(Keys.TYPE, label.name());

		node.setProperty(Keys.FQN, unit.getName());
		node.setProperty(Keys.VISIBILITY, Visibility.resolve(unit.getModifiers()));
		node.setProperty(Keys.ORIGIN, getOrigin(unit).toString());

		if (unit.hasTag(TypeParametersTag.NAME)) {
			TypeParametersTag typeParametersTag = (TypeParametersTag) unit.getTag(TypeParametersTag.NAME);
			node.setProperty(Keys.TYPE_PARAMS, typeParametersTag.getTypeParameters().toString());
		}

		return node;
	}

	private Origin getOrigin(final SootClass unit) {
		if (unit.isApplicationClass()) {
			return Origin.APP;
		}
		if (unit.isJavaLibraryClass()) {
			return Origin.JAVA;
		}
		if (unit.isLibraryClass()) {
			return Origin.LIB;
		}
		if (unit.isPhantomClass()) {
			return Origin.UNKNOWN;
		}
		throw new IllegalStateException("Unknown origin mapping");
	}

}
