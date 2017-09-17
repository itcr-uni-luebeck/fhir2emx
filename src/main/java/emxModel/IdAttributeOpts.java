/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

public enum IdAttributeOpts {
	
	TRUE, FALSE, AUTO;
	
	/**
	 * Representation of the idAttribute attribute as defined in EMX
	 */
	public String toString(){
		
		if(this.equals(AUTO)){
			return this.name();
		}
		if(this.equals(FALSE)){
			return "FALSE";
		}
		if(this.equals(TRUE)){
			return "TRUE";
		}
		
		return null;
	}

}
