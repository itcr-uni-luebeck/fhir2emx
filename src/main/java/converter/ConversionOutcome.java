/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package converter;

import java.util.ArrayList;
import java.util.HashMap;

import org.hl7.fhir.dstu3.model.codesystems.IssueSeverity;

public class ConversionOutcome {
	
	private HashMap<String, String> emxTables;
	
	private ArrayList<Issue> issues;
	
	ConversionOutcome(){
		this.issues = new ArrayList<Issue>();
	}

	public ConversionOutcome addError(IssueSeverity severity, String location, String message){
		this.issues.add(new Issue(severity, location, message));
		return this;
	}
	

	/**
	 * @return the issues
	 */
	public ArrayList<Issue> getIssues() {
		return issues;
	}

	/**
	 * @param issues the issues to set
	 */
	void setIssues(ArrayList<Issue> errors) {
		this.issues = errors;
	}

	/**
	 * @return the emxTables
	 */
	public HashMap<String, String> getEmxTables() {
		return emxTables;
	}

	/**
	 * @param emxTables the emxTables to set
	 */
	public void setEmxTables(HashMap<String, String> emxTables) {
		this.emxTables = emxTables;
	}

	public boolean hasErrors(){
		for(Issue issue:this.getIssues()){
			if(issue.getSeverity().equals(IssueSeverity.FATAL) || issue.getSeverity().equals(IssueSeverity.ERROR)){
				return true;
			}
		}
		return false;
	}
}
