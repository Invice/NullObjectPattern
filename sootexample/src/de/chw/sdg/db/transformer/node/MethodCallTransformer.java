package de.chw.sdg.db.transformer.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import de.chw.sdg.db.transformer.util.Keys;
import de.chw.sdg.db.transformer.util.NamingUtil;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.DynamicInvokeExpr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticInvokeExpr;

public class MethodCallTransformer extends AbstractNodeTransformer {

	private final Multimap<SootMethod, Node> invocations = ArrayListMultimap.create();
	private final Multimap<SootMethod, SootMethod> aggregatedInvocations = ArrayListMultimap.create();
	// labels
	public static final Label TYPE_LABEL = NamingUtil.INSTANCE.getLabel("methodcall");
	public static final Label TYPE_WITH_RETURN_VALUE_LABEL = NamingUtil.INSTANCE.getLabel("methodcallwithreturnvalue");
	private static final Label CTOR_TYPE_LABEL = NamingUtil.INSTANCE.getLabel("constructorcall");

	public MethodCallTransformer(final GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService);
	}

	public Node transform(final InvokeExpr invokeExpr, final SootMethod method) {
		Node node = createNode();

		transform(node, invokeExpr, method);

		return node;
	}

	public String transform(final Node node, final InvokeExpr invokeExpr, final SootMethod method) {
		if (node == null) {
			throw new IllegalArgumentException("node is null");
		}
		SootMethod methodDecl = invokeExpr.getMethod();
		String name = buildName(invokeExpr, method);
		String buildDisplayName = buildDisplayName(invokeExpr);

		Type returnType = methodDecl.getReturnType();
		Label label;
		if (methodDecl.isConstructor()) {
			label = CTOR_TYPE_LABEL;
		} else if (returnType.equals(VoidType.v())) {
			label = TYPE_LABEL;
		} else {
			label = TYPE_WITH_RETURN_VALUE_LABEL;
		}

		node.setProperty(Keys.DISPLAYNAME, buildDisplayName);
		node.setProperty(Keys.NAME, name);
		node.setProperty(Keys.TYPE, label.name());
		node.addLabel(label);
		// custom properties
		node.setProperty(Keys.FQN, NamingUtil.INSTANCE.buildFQN(invokeExpr.getMethod()));
		node.setProperty(Keys.RETURN, returnType.toString());
		node.setProperty(Keys.ARGUMENTS_COUNT, invokeExpr.getArgCount());

		String[] arguments = new String[invokeExpr.getArgCount()];
		for (int i = 0; i < invokeExpr.getArgCount(); i++) {
			Value arg = invokeExpr.getArgs().get(i);
			// arg.getType();
			arguments[i] = arg.toString();
		}
		node.setProperty(Keys.ARGUMENTS, arguments);

		String caller = extractCaller(invokeExpr);
		node.setProperty(Keys.CALLER, caller);

		invocations.put(methodDecl, node);
		aggregatedInvocations.put(method, methodDecl);

		return buildDisplayName;
	}

	private String buildName(final InvokeExpr invokeExpr, final SootMethod method) {
		if (invokeExpr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr castedInvokeExpr = (InstanceInvokeExpr) invokeExpr;

			// Type type = castedInvokeExpr.getType();

			if (castedInvokeExpr.getMethod().isConstructor()) {
				// there are at least the following cases:
				// specialinvoke this.<java.lang.Object: void <init>()>()
				// specialinvoke this.<de.chw.sdg.example.constructor.Constructor: void <init>(int,float)>(param, 0.0F)
				// specialinvoke $r0.<de.chw.sdg.example.constructor.Constructor: void <init>()>()
				String caller = castedInvokeExpr.getBase().toString();
				SootClass sourceDeclaringClass = method.getDeclaringClass();
				SootClass targetDeclaringClass = castedInvokeExpr.getMethod().getDeclaringClass();
				if (caller.equals("this")) {
					boolean equalClasses = sourceDeclaringClass.getName().equals(targetDeclaringClass.getName());
					if (equalClasses) {
						return "this";
					} else {
						return "super";
					}
				}
			}
		}
		return invokeExpr.getMethod().getName();	// just the method's name w/o any arguments
	}

	private String buildDisplayName(final InvokeExpr invokeExpr) {
		String args = NamingUtil.INSTANCE.buildArguments(invokeExpr);
		return invokeExpr.getMethod().getName() + args;
	}

	private String extractCaller(final InvokeExpr invokeExpr) {
		String caller;
		if (invokeExpr instanceof StaticInvokeExpr) {
			StaticInvokeExpr castedInvokeExpr = (StaticInvokeExpr) invokeExpr;
			caller = castedInvokeExpr.getMethodRef().declaringClass().toString();
		} else if (invokeExpr instanceof InstanceInvokeExpr) {
			InstanceInvokeExpr castedInvokeExpr = (InstanceInvokeExpr) invokeExpr;
			caller = castedInvokeExpr.getBase().toString();
		} else if (invokeExpr instanceof DynamicInvokeExpr) {
			caller = invokeExpr.toString();
		} else {
			caller = "unknown caller in " + invokeExpr;
		}
		return caller;
	}

	public Multimap<SootMethod, Node> getInvocations() {
		return invocations;
	}

	/**
	 * @key source SootMethod
	 * @value target SootMethod
	 *
	 * @return
	 */
	public Multimap<SootMethod, SootMethod> getAggregatedInvocations() {
		return aggregatedInvocations;
	}
}
