/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

public enum EMXLangCode {
	
	EN ("English"),
	PT ("Portugese"),
	ES ("Spanish"),
	DE ("German"),
	IT ("Italian"),
	FR ("French"),
	NL ("Dutch"),
	XX ("MyLanguage");
	
	private final String languageName;
	
	EMXLangCode(String langName){
		this.languageName = langName;
	}

	public String languageName(){
		return this.languageName;
	}
	
	/**
	 * Returns the language code in lower case form as used in MOLGENIS field suffixes.
	 */
	public String toString(){
		return this.name().toLowerCase();
	}
	
	public static boolean isSupported(String langcode){
		if(valueOf(langcode.toUpperCase()) != null){
			return true;
		}else{
			return false;
		}
	}
	
	public static EMXLangCode getEnumValue(String langCode){
		return valueOf(langCode.toUpperCase());
	}
	
}
