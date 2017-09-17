/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package JsonModels;

public class Login {
	
	public String username;
	public String password;
	
	public Login(String username, String password){
		this.username = username;
		this.password = password;
	}

}
