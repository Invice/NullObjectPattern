package com.tnr.neo4j.java.nullobject.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StringUtil {
	
	/**
	 * Transforms the classpath of a given class to add a prefix
	 * @param prefix (for example "Abstract", "Real", ...)
	 * @param classPath the old classpath (fqn) of a class
	 * @return
	 */
	public static String addPrefixToClass(String prefix, final String classPath){
		
		List<String> parts = new ArrayList<>(Arrays.asList(classPath.split("\\.")));
		
		int lastString = parts.size()-1;
		
		String transformedClassPath = "";
		for (int i = 0; i < lastString; i++){
			transformedClassPath = transformedClassPath + parts.get(i) + ".";
		}
		
		transformedClassPath = transformedClassPath + prefix + parts.get(lastString);

		return transformedClassPath;
	}
	
		
	public static String addPrefixToClass(String prefix, final Object classPath){
		return addPrefixToClass(prefix, (String) classPath);
	}
	
	/**
	 * Transforms the package string of a given method to match a transformed candidate.
	 * @param classPath the new classpath
	 * @param methodFqn the old methodpath (fqn)
	 * @return
	 */
	public static String addClassPathToMethod(String classPath, final String methodFqn){
		
		/*
		 * TODO: check for transformed arguments
		 */
		String[] method = methodFqn.split("\\(");
		List<String> parts = new ArrayList<>(Arrays.asList(method[0].split("\\.")));
		
		return classPath + "." + parts.get(parts.size()-1) + "(" + method[1];
	}
	
	/**
	 * Extracts the package path from a given class path.
	 * @param classPath
	 * @return
	 */
	public static String extractPackagePath(final String classPath){
		
		List<String> parts = new ArrayList<>(Arrays.asList(classPath.split("\\.")));
		String packagePath = "";
		
		for (int i = 0; i < parts.size()-2; i++) {
			packagePath = packagePath + parts.get(i) + ".";
		}
		packagePath = packagePath + parts.get(parts.size()-2);
		return packagePath;
	}
	
	/**
	 * Builds a new displayname for an outgoing assignment node using the new vartype, the old name and the assigned fields name.
	 * @param vartype the new vartype to use
	 * @param displayname the old displayname
	 * @param fieldName the name of the field in the right value
	 * @return
	 */
	public static String buildOutgoingDisplayname(final String vartype, final String displayname, final String fieldName){
		
		String[] tmp = displayname.split("\\:");
		String newDisplayname = tmp[0]+ ": " + vartype + " " + fieldName + ">";
		
		return newDisplayname;
	}
	
	/**
	 * Builds a new displayname for an incoming assignment node using the new vartype, the old name and the assigned fields name.
	 * @param vartype the new vartype to use
	 * @param displayname the old displayname
	 * @param fieldName the name of the field in the right value
	 * @return
	 */
	public static String buildIncomingDisplayname(final String vartype, final String displayname, final String fieldName){
		
		String[] tmp = displayname.split("\\:");
		String[] tmp2 = tmp[1].split("\\>");
		String newDisplayName = tmp[0] + ": " + vartype + " " + fieldName + ">" + tmp2[1]; 
		
		return newDisplayName;
	}
	
	
	
	
	
	
	/**
	 * Builds a new right value string for MethodCallWithReturnValue nodes.
	 * @param properties the properties of the node
	 * @return
	 */
	public static String buildRightValue (String abstractFqn, Map<String, Object> properties){
		
		String rightValue = "virtualinvoke " + properties.get("caller") + ".<"
				+ abstractFqn + ": " + properties.get("vartype") 
				+ properties.get("displayname").toString().split("\\=")[1] + ">()";
		
		return rightValue;
	}
	
	
	public static String cutMethodName(final String methodFqn){
		
		String[] tmp = methodFqn.split("\\(");
		String fqn = extractPackagePath(tmp[0]);
		
		return fqn;
	}
	
	
	public static void main(String[] args) {
		
		String exampleClassPath = "de.tnr.sdg.example.transformedCache.Cache";
		String exampleMethodPath = exampleClassPath + ".reset()";
		String exampleMethodPath2 = exampleClassPath + ".put(java.Lang.String)";
		
		String exampleVarType = "de.tnr.sdg.example.transformedCache.AbstractText";
		String exampleDisplayName1 = "temp$0 = this.<de.tnr.sdg.example.transformedCache.MainClass: de.tnr.sdg.example.transformedCache.Cache cache>";
		String exampleDisplayName2 = "this.<de.tnr.sdg.example.cache.MainClass: de.tnr.sdg.example.cache.Cache cache2> = temp$0";

		Map<String,Object> map = new HashMap<>();
		map.put("caller", "temp$3");
		map.put("fqn", "de.tnr.sdg.example.cache.AbstractText.getText()");
		map.put("vartype", "java.lang.String");
		map.put("displayname", "temp$7 = getText()");
		
//		System.out.println(buildRightValue(exampleVarType, map).equals("virtualinvoke temp$3.<de.tnr.sdg.example.transformedCache.AbstractText: java.lang.String getText()>()"));
		
//		System.out.println(addPrefixToClass("Real", exampleClassPath));
//		System.out.println(addPrefixToClass("Abstract", exampleClassPath));
//		System.out.println(addPrefixToClass("Null", exampleClassPath));
//		
//		System.out.println(addClassPathToMethod(addPrefixToClass("Abstract", exampleClassPath), exampleMethodPath));
//		System.out.println(addClassPathToMethod(addPrefixToClass("Null", exampleClassPath), exampleMethodPath2));
		
//		System.out.println(extractPackagePath(exampleClassPath));
//		System.out.println(cutMethodName(exampleMethodPath));
		
//		System.out.println(buildOutgoingDisplayname(addPrefixToClass("Abstract", exampleClassPath), exampleDisplayName1, "cache"));
//		System.out.println(buildOutgoingDisplayname(addPrefixToClass("Abstract", exampleClassPath), exampleDisplayName1, "cache").equals(exampleDisplayName2));
		
		System.out.println(buildIncomingDisplayname(addPrefixToClass("Abstract", exampleClassPath), exampleDisplayName2, "cache"));
		
		
		
	}
}
