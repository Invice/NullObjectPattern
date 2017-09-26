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
		realFqn = StringUtil.addPrefixToClass(realPrefix, candidateProperties.get(SDGPropertyKey.FQN));
		nullFqn = StringUtil.addPrefixToClass(nullPrefix, candidateProperties.get(SDGPropertyKey.FQN));	
		abstractFqn = StringUtil.addPrefixToClass(abstractPrefix, candidateProperties.get(SDGPropertyKey.FQN));
	}
	
	
	public String getRealPrefix() {
		return realPrefix;
	}


	public void setRealPrefix(String realPrefix) {
		this.realPrefix = realPrefix;
	}


	public String getAbstractPrefix() {
		return abstractPrefix;
	}


	public void setAbstractPrefix(String abstractPrefix) {
		this.abstractPrefix = abstractPrefix;
	}


	public String getNullPrefix() {
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
