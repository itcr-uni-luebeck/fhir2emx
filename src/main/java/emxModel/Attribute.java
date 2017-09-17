/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

public class Attribute {

	private Entity entity;
	
	private String name;
	
	private EMXDataType datatype;
	
	private Entity refEntity;
	
	private boolean isNillable;
	
	private IdAttributeOpts isIdAttribute;
	
	private String description;
	
	private HashMap<EMXLangCode,String> descriptions;
	
	private Integer rangeMin;
	
	private Integer rangeMax;
	
	private boolean isLookupAttribute;
	
	private String label;
	
	private HashMap<EMXLangCode,String> labels;
	
	private boolean isAggregateable;
	
	private boolean isLabelAttribute;
	
	private boolean isReadOnly;
	
	private ArrayList<Tag> tags;
	
	private String validationExpression;
	
	private String defaultValue;
	
	private Attribute partOfAttribute;
	
	private String expression;
	
	private ArrayList<String> enumOptions;
	
	public Attribute(Entity entity, String name) throws InvalidNameException{
		
		this.setEntity(entity);
		if(!EMX.isValidEMXName(name)){
			throw new InvalidNameException(name+" is no valid EMX name");
		}
		this.name = name;
		
		this.setDataType(EMXDataType.STRING);
		this.setNillable(false);
		this.setIsIdAttribute(IdAttributeOpts.FALSE);
		this.setLookupAttribute(false);
		this.setLabelAttribute(false);
		
		this.descriptions = new HashMap<EMXLangCode,String>();
		this.labels = new HashMap<EMXLangCode,String>();
		this.tags = new ArrayList<Tag>();
		this.enumOptions = new ArrayList<String>();
		
		
	}
	
	public void addEnumOption(String value){
		this.enumOptions.add(value);
	}
	
	public Tag addTag(Tag tag){
		this.tags.add(tag);
		return tag;
	}
	
	
	public Tag addTag(String identifier, String label) throws NamingException{
		return this.addTag(new Tag(identifier,label,this.entity.getInPackage().getRoot().getTagNamespace()));
	}
	
	
	
	public String getName(){
		return this.name;
	}
	
	
	public Attribute setDescription(EMXLangCode langCode, String description){
		this.descriptions.put(langCode, description);
		return this;
	}
	
	
	public Attribute setDescription(String langCode, String description) throws IllegalArgumentException{
		EMXLangCode code = EMXLangCode.valueOf(langCode.toUpperCase());
		this.descriptions.put(code, description);
		return this;
	}
	
	
	public Attribute setDescription(String description){
		this.description = description;
		return this;
	}
	
	
	public String getDescription(){
		if(this.description == null){
			return "";
		}
		return this.description;
	}
	
	
	public String getDescription(EMXLangCode langCode){
		if(this.descriptions.get(langCode) == null){
			return "";
		}
		return this.descriptions.get(langCode);
	}
	
	
	public String getDescription(String langCode) throws IllegalArgumentException{
		EMXLangCode code = EMXLangCode.valueOf(langCode.toUpperCase());
		return this.descriptions.get(code);
	}
	
	
	
	
	public Attribute setLabel(EMXLangCode langCode, String description){
		this.labels.put(langCode, description);
		return this;
	}
	
	
	public Attribute setLabel(String langCode, String description) throws IllegalArgumentException{
		EMXLangCode code = EMXLangCode.valueOf(langCode.toUpperCase());
		this.labels.put(code, description);
		return this;
	}
	
	
	public Attribute setLabel(String description){
		this.label = description;
		return this;
	}
	
	
	public String getLabel(){
		if(this.label == null){
			return "";
		}
		return this.label;
	}
	
	
	public String getLabel(EMXLangCode langCode){
		if(this.labels.get(langCode) == null){
			return "";
		}
		return this.labels.get(langCode);
	}
	
	
	public String getLabel(String langCode) throws IllegalArgumentException{
		EMXLangCode code = EMXLangCode.valueOf(langCode.toUpperCase());
		return this.labels.get(code);
	}

	
	public int getRangeMin() {
		if(rangeMin == null){
			return 0;
		}
		return rangeMin;
	}
	
	public String getRangeMinAsString(){
		if(this.rangeMin == null){
			return "";
		}
		return rangeMin.toString();
	}

	
	public void setRangeMin(int rangeMin) {
		this.rangeMin = rangeMin;
	}

	
	public int getRangeMax() {
		if(rangeMax == null){
			return 0;
		}
		return rangeMax;
	}
	
	public String getRangeMaxAsString(){
		if(this.rangeMax == null){
			return "";
		}
		return rangeMax.toString();
	}

	
	public void setRangeMax(int rangeMax) {
		this.rangeMax = rangeMax;
	}

	
	public boolean isLookupAttribute() {
		return isLookupAttribute;
	}

	
	public void setLookupAttribute(boolean isLookupAttribute) {
		this.isLookupAttribute = isLookupAttribute;
	}

	
	public boolean isAggregateable() {
		return isAggregateable;
	}

	
	/**
	 * @return the tags
	 */
	public ArrayList<Tag> getTags() {
		return tags;
	}

	public void setAggregateable(boolean isAggregateable) {
		this.isAggregateable = isAggregateable;
	}

	
	public boolean isLabelAttribute() {
		return isLabelAttribute;
	}

	
	/**
	 * @return the enumOptions
	 */
	public ArrayList<String> getEnumOptions() {
		return enumOptions;
	}
	
	public String getEnumOptionsAsCSV(){
		if(this.enumOptions.isEmpty()){
			return "";
		}
		String csv = this.enumOptions.get(0);
		for(int i = 1; i<this.enumOptions.size();i++){
			csv = csv.concat(","+this.enumOptions.get(i));
		}
		return csv;
	}

	public void setLabelAttribute(boolean isLabelAttribute) {
		this.isLabelAttribute = isLabelAttribute;
	}

	
	public boolean isReadOnly() {
		return isReadOnly;
	}

	
	public void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	
	public String getValidationExpression() {
		if(this.validationExpression == null){
			return "";
		}
		return validationExpression;
	}

	
	public void setValidationExpression(String validationExpression) {
		this.validationExpression = validationExpression;
	}

	
	public IdAttributeOpts getIsIdAttribute() {
		return isIdAttribute;
	}

	/**
	 * Only to be used by Entity class!
	 * @param isIdAttribute
	 */
	void setIsIdAttribute(IdAttributeOpts isIdAttribute) {
		this.isIdAttribute = isIdAttribute;
	}

	public boolean isNillable() {
		return isNillable;
	}

	
	public void setNillable(boolean isNillable) {
		this.isNillable = isNillable;
	}

	

	public EMXDataType getDataType() {
		return datatype;
	}

	

	public void setDataType(EMXDataType datatype) {
		this.datatype = datatype;
	}

	

	public Entity getRefEntity() {
		return refEntity;
	}

	
	public void setRefEntity(Entity refEntity) {
		this.refEntity = refEntity;
	}

	
	public Entity getEntity() {
		return entity;
	}

	
	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	
	public String getExpression() {
		if(this.expression == null){
			return "";
		}
		return expression;
	}

	
	public void setExpression(String expression) {
		this.expression = expression;
	}

	
	public Attribute getPartOfAttribute() {
		return partOfAttribute;
	}

	
	public void setPartOfAttribute(Attribute partOfAttribute) {
		this.partOfAttribute = partOfAttribute;
	}

	
	public String getDefaultValue() {
		if(this.defaultValue == null){
			return "";
		}
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) throws Exception {
		if(testDataTypeConformance(this.datatype,defaultValue)){
			this.defaultValue = defaultValue;
		}else{
			throw new Exception(defaultValue+" is not of DataType "+this.datatype.toString());
		}
	}
	
	public static boolean testDataTypeConformance(EMXDataType dataType, String value){
		
		//Currently only stub function
		
		return true;
	}
}
