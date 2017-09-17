/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

public enum EMXDataType {
	
	STRING, TEXT, INT, LONG, DECIMAL, BOOL, DATE, DATETIME, XREF, MREF, CATEGORICAL, CATEGORICAL_MREF, COMPOUND, FILE, EMAIL, ENUM, HYPERLINK;
	
	/**
	 * Representation of the DataType as defined in EMX
	 */
	public String toString(){
		
		return this.name().toLowerCase();
	}

}
