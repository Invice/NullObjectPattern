package com.tnr.neo4j.java.nullobject.transformation.util;

import java.util.Map;

import com.tnr.neo4j.java.nullobject.util.SDGPropertyKey;
import com.tnr.neo4j.java.nullobject.util.StringUtil;

public class PropertyContainer {
	/*
	 * Set default name prefixes for new classes.
	 */
	private Map<String,Object> candidateProperties;
	
	private String realPrefix = "Real";
	private String abstractPrefix = "Abstract";
	private String nullPrefix = "Null";
	
	private int prefixCount = -1;
	
	private String realFqn;
	private String nullFqn;
	private String abstractFqn;
	private String candidateFqn;
	
	public PropertyContainer(Map<String,Object> candidateProperties){
		
		this.candidateProperties = candidateProperties;
		candidateFqn = (String) candidateProperties.get(SDGPropertyKey.FQN);
		updateFqns();
	}	
	
	public void updateFqns(){
		realFqn = StringUtil.addPrefixToClass(getRealPrefix(), candidateProperties.get(SDGPropertyKey.FQN));
		nullFqn = StringUtil.addPrefixToClass(getNullPrefix(), candidateProperties.get(SDGPropertyKey.FQN));	
		abstractFqn = StringUtil.addPrefixToClass(getAbstractPrefix(), candidateProperties.get(SDGPropertyKey.FQN));
	}
	
	/**
	 * Adds an increasing number to the prefixes and updates the fqns.
	 */
	public void increasePrefixNum(){
		prefixCount++;
		updateFqns();
		System.out.println("FQN already in use. Increasing prefixCount to " + prefixCount + ".");
	}
	
	public String getRealPrefix() {
		if (prefixCount >= 0) {
			return realPrefix + prefixCount;
		}
		return realPrefix;
	}


	public void setRealPrefix(String realPrefix) {
		this.realPrefix = realPrefix;
	}


	public String getAbstractPrefix() {
		if (prefixCount >= 0) {
			return abstractPrefix + prefixCount;
		}
		return abstractPrefix;
	}


	public void setAbstractPrefix(String abstractPrefix) {
		this.abstractPrefix = abstractPrefix;
	}


	public String getNullPrefix() {
		if (prefixCount >= 0) {
			return nullPrefix + prefixCount;
		}
		return nullPrefix;
	}


	public void setNullPrefix(String nullPrefix) {
		this.nullPrefix = nullPrefix;
	}


	public Map<String, Object> getCandidateProperties() {
		return candidateProperties;
	}


	public String getRealFqn() {
		return realFqn;
	}


	public String getNullFqn() {
		return nullFqn;
	}


	public String getAbstractFqn() {
		return abstractFqn;
	}


	public String getCandidateFqn() {
		return candidateFqn;
	}
}
