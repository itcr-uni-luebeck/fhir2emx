/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package JsonModels;

public class IncomingRequest {

	public String serverbase;

	public String questionnaireid;

	public String molgenisurl;

	public String molgenisuser;

	public String molgenispw;
	
	public boolean generateTags;

	public IncomingRequest(String serverbase, String questionnaireid, String molgenisurl, String molgenisuser,
			String molgenispw, boolean generateTags) {
		super();
		this.serverbase = serverbase;
		this.questionnaireid = questionnaireid;
		this.molgenisurl = molgenisurl;
		this.molgenisuser = molgenisuser;
		this.molgenispw = molgenispw;
		this.generateTags = generateTags;
	}

	public IncomingRequest() {
		super();
	}

}
