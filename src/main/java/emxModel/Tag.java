/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

import java.net.URL;
import java.util.HashMap;

import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

public class Tag {
	
	
	
	private String identifier;
	
	private String label;
	
	private URL objectIRI;
	
	private String relationLabel;
	
	private URL relationIRI;
	
	private String codeSystem;
	
	Tag(String identifier, String label, HashMap<String,Tag> tagNamespace) throws NamingException{
		
		if(!EMX.isValidEMXName(identifier)){
			throw new InvalidNameException(identifier+" is no valid EMX name!");
		}
		if(tagNamespace.containsKey(identifier)){
			if(tagNamespace.get(identifier).getLabel().equals(label)){
				return;
			}
			//Trying to overwrite existing Tag, throw Exception
			throw new NameAlreadyBoundException("Tag "+identifier+"already exists");
		}
		
		tagNamespace.put(identifier, this);
		this.identifier = identifier;
		this.setLabel(label);
		
	}
	

	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return the objectIRI
	 */
	public URL getObjectIRI() {
		return objectIRI;
	}


	public String getObjectIRIAsString() {
		if (objectIRI == null) {

			return "";
		}
		return objectIRI.toString();
	}
	
	/**
	 * @param objectIRI the objectIRI to set
	 */
	public void setObjectIRI(URL objectIRI) {
		this.objectIRI = objectIRI;
	}

	/**
	 * @return the relationLabel
	 */
	public String getRelationLabel() {
		if(relationLabel == null){
			return "";
		}
		return relationLabel;
	}

	/**
	 * @param relationLabel the relationLabel to set
	 */
	public void setRelationLabel(String relationLabel) {
		this.relationLabel = relationLabel;
	}

	/**
	 * @return the relationIRI
	 */
	public URL getRelationIRI() {
		return relationIRI;
	}
	
	
	public String getRelationIRIAsString() {
		if (relationIRI == null) {

			return "";
		}
		return relationIRI.toString();
	}

	/**
	 * @param relationIRI the relationIRI to set
	 */
	public void setRelationIRI(URL relationIRI) {
		this.relationIRI = relationIRI;
	}

	/**
	 * @return the codeSystem
	 */
	public String getCodeSystem() {
		if(codeSystem == null){
			return "";
		}
		return codeSystem;
	}

	/**
	 * @param codeSystem the codeSystem to set
	 */
	public void setCodeSystem(String codeSystem) {
		this.codeSystem = codeSystem;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	
	

}
