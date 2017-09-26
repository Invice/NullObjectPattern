package com.tnr.neo4j.java.nullobject.util;

public class Constants {
	public static final String GraphDatabaseLocation = "C:\\Users\\Tim-Niklas Reck\\Desktop\\Bachelorarbeit\\sootexample\\databases\\";
			
	public static final String antDB = "org.apache.tools.ant.Main";		
	public static final String cacheDB = "de.tnr.sdg.example.cache.MainClass";
	
	/*
	 * Set default name prefixes for new classes.
	 */
	public static String realPrefix = "Real";
	public static String abstractPrefix = "Abstract";
	public static String nullPrefix = "Null";
	
	public static void resetPrefixes(){
		Constants.realPrefix = "Real";
		Constants.abstractPrefix = "Abstract";
		Constants.nullPrefix = "Null";
	}
	
}