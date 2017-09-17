/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package server;

import java.util.ArrayList;

public class TableResult {
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the content
	 */
	public ArrayList<String[]> getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 */
	public void setContent(ArrayList<String[]> content) {
		this.content = content;
	}

	public TableResult(String name, ArrayList<String[]> content) {
		super();
		this.name = name;
		this.content = content;
	}

	public String name;
	
	public ArrayList<String[]> content;
	

}
