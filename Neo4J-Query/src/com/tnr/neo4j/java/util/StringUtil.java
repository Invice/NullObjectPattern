package com.tnr.neo4j.java.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
	 * @param methodPath the old methodpath (fqn)
	 * @return
	 */
	public static String addClassPathToMethod(String classPath, final String methodPath){
		
		/*
		 * TODO: check for transformed arguments
		 */
		String[] method = methodPath.split("\\(");
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
	
	
	public static void main(String[] args) {
		
		String exampleClassPath = "de.tnr.sdg.example.transformedCache.Cache";
		String exampleMethodPath = exampleClassPath + ".reset()";
		String exampleMethodPath2 = exampleClassPath + ".put(java.Lang.String)";
		
		
//		System.out.println(addPrefixToClass("Real", exampleClassPath));
//		System.out.println(addPrefixToClass("Abstract", exampleClassPath));
//		System.out.println(addPrefixToClass("Null", exampleClassPath));
//		
//		System.out.println(addClassPathToMethod(addPrefixToClass("Abstract", exampleClassPath), exampleMethodPath));
//		System.out.println(addClassPathToMethod(addPrefixToClass("Null", exampleClassPath), exampleMethodPath2));
		
		System.out.println(extractPackagePath(exampleClassPath));
	}
}
