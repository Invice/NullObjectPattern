package de.chw.sdg.db.transformer.util;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Label;

import com.google.common.base.Joiner;

import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.InvokeExpr;

public class NamingUtil {

	public static final NamingUtil INSTANCE = new NamingUtil();

	private final LabelUtil labelUtil = LabelUtil.INSTANCE;
	private final PropertyKeyUtil propertyKeyUtil = PropertyKeyUtil.INSTANCE;

	private NamingUtil() {
		// utility class
	}

	public Label getLabel(String key) {
		return labelUtil.getLabel(key);
	}

	public String getPropertyKey(String key) {
		return propertyKeyUtil.getPropertyKey(key);
	}

	public String buildParameterTypes(SootMethod unit) {
		List<String> params = new ArrayList<>();
		for (Type p : unit.getParameterTypes()) {
			params.add(p.toString());
		}
		return "(" + Joiner.on(",").join(params) + ")";
	}

	/**
	 * @param unit
	 * @return ( arg0, arg1, ...)
	 */
	public String buildArguments(InvokeExpr unit) {
		List<String> args = new ArrayList<>();
		for (Value arg : unit.getArgs()) {
			args.add(arg.toString());
		}
		return "(" + Joiner.on(",").join(args) + ")";
	}

	public String buildFQN(SootMethod methodDecl) {
		return methodDecl.getDeclaringClass().getName() + "." + methodDecl.getName()
		+ NamingUtil.INSTANCE.buildParameterTypes(methodDecl);

		// example signature by soot: <kieker.analysis.AnalysisController: void connect(kieker.analysis.plugin.AbstractPlugin,java.lang.String,kieker.analysis.plugin.AbstractPlugin,java.lang.String)>
		//		return methodDecl.getSignature();
	}
}
