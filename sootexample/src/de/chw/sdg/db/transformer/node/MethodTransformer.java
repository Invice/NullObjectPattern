package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.ModifierUtil;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;

public class MethodTransformer extends AbstractNodeTransformer {

	private static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("methoddeclaration");
	private static final Label CTOR_LABEL = NamingUtil.INSTANCE.getLabel("constructordeclaration");

	public MethodTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final SootMethod unit) {
		Node node;

		if (unit.isConstructor()) {
			node = createNode(CTOR_LABEL);
			node.setProperty(Keys.DISPLAYNAME, "ctor" + NamingUtil.INSTANCE.buildParameterTypes(unit));
			node.setProperty(Keys.NAME, "ctor");
		} else {
			node = createNode(TYPE_LABEL);
			node.setProperty(Keys.DISPLAYNAME, unit.getName() + NamingUtil.INSTANCE.buildParameterTypes(unit));
			node.setProperty(Keys.NAME, unit.getName());
			node.setProperty(Keys.RETURN_TYPE, unit.getReturnType().toString());
		}

		node.setProperty(Keys.FQN, NamingUtil.INSTANCE.buildFQN(unit));

		node.setProperty(Keys.PARAMETER_COUNT, unit.getParameterCount());
		node.setProperty(Keys.IS_DECLARED, unit.isDeclared());

		int modifiers = unit.getModifiers();
		ModifierUtil.setModifiers(node, modifiers);

		for (int i = 0; i < unit.getParameterTypes().size(); i++) {
			Type parameterType = unit.getParameterType(i);
			node.setProperty("p" + i, parameterType.toString());
		}

		String[] exceptions = new String[unit.getExceptions().size()];
		for (int i = 0; i < unit.getExceptions().size(); i++) {
			SootClass exception = unit.getExceptions().get(i);
			exceptions[i] = exception.getName();
		}
		node.setProperty(Keys.THROWS, exceptions);

		return node;
	}

}
