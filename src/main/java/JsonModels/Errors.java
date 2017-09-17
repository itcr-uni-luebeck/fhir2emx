/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package JsonModels;

import java.util.ArrayList;
import java.util.List;

import converter.Issue;

public class Errors {
	
	public List<Issue> issues;

	public Errors() {
		this.issues = new ArrayList<Issue>();
	}

}
