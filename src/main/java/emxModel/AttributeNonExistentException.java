/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

public class AttributeNonExistentException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4754514358134396562L;

	public AttributeNonExistentException() {
		
	}

	public AttributeNonExistentException(String message) {
		super(message);
		
	}

	public AttributeNonExistentException(Throwable cause) {
		super(cause);
		
	}

	public AttributeNonExistentException(String message, Throwable cause) {
		super(message, cause);
		
	}

	public AttributeNonExistentException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		
	}

}
