/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package converter;

import org.hl7.fhir.dstu3.model.codesystems.IssueSeverity;

public class Issue {
	
	private IssueSeverity severity;
	
	private String location;
	
	private String message;

	public Issue(IssueSeverity severity, String location, String message) {
		super();
		this.severity = severity;
		this.location = location;
		this.message = message;
	}

	/**
	 * @return the severity
	 */
	public IssueSeverity getSeverity() {
		return severity;
	}

	/**
	 * @param severity the severity to set
	 */
	public void setSeverity(IssueSeverity severity) {
		this.severity = severity;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}
