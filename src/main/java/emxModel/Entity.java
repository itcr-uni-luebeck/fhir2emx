/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converter.QuestionnaireConverter;

public class Entity {
	
	private String name;
	
	private Entity extendsEntity;
	
	private EMXPackage inPackage;
	
	private boolean isAbstract;
	
	private String description;
	
	private HashMap<EMXLangCode,String> descriptions;
	
	private ArrayList<Tag> tags;
	
	private Map<String,Attribute> attributes;
	
	private ArrayList<String> instanceNamespace;
	
	private ArrayList<Instance> instances;
	
	final static Logger logger = LoggerFactory.getLogger(QuestionnaireConverter.class);
	
	Entity(String name) throws InvalidNameException{
		
		if(!EMX.isValidEMXName(name)){
			throw new InvalidNameException(name+" is no valid EMX name");
		}
		this.name = name;
		this.descriptions = new HashMap<EMXLangCode,String>();
		this.tags = new ArrayList<Tag>();
		this.attributes = new ConcurrentHashMap<String,Attribute>();
		this.instances = new ArrayList<Instance>();
		this.instanceNamespace = new ArrayList<String>();
		
	}

	
	public Attribute addAttribute(String name) throws NamingException{
		
		if(this.attributes.keySet().contains(name)){
			throw new NameAlreadyBoundException(name +" already exists in entity "+this.getFullName());
		}
		Attribute a = new Attribute(this,name);
		a.setEntity(this);
		this.attributes.put(name,a);
		
		return a;
	}
	
	
	public Tag addTag(Tag tag){
		this.tags.add(tag);
		return tag;
	}
	
	
	public Tag addTag(String identifier, String label) throws NamingException{
		return this.addTag(new Tag(identifier,label,this.getInPackage().getRoot().getTagNamespace()));
	}
	
	
	public boolean isAttributeNameAvailable(String name){
		if(!EMX.isValidEMXName(name)){
			return false;
		}
		if(this.attributes.keySet().contains(name)){
			return false;
		}
		
		return true;
	}
	
	
	public Entity setDescription(EMXLangCode langCode, String description){
		this.descriptions.put(langCode, description);
		return this;
	}
	
	
	public Entity setDescription(String langCode, String description) throws IllegalArgumentException{
		EMXLangCode code = EMXLangCode.valueOf(langCode.toUpperCase());
		this.descriptions.put(code, description);
		return this;
	}
	
	
	public Entity setDescription(String description){
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
	
	
	public String getName() {
		return name;
	}
	
	
	public String getFullName(){
		return this.inPackage.getFullName()+"_"+this.name;
	}

	
	public void setName(String name) {
		this.name = name;
	}

	
	public Entity getExtendsEntity() {
		return extendsEntity;
	}

	
	public void setExtendsEntity(Entity extendsEntity) {
		this.extendsEntity = extendsEntity;
	}

	
	public EMXPackage getInPackage() {
		return inPackage;
	}

	
	public void setInPackage(EMXPackage inPackage) {
		this.inPackage = inPackage;
	}

	
	public boolean isAbstract() {
		return isAbstract;
	}

	
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	
	public ArrayList<Tag> getTags() {
		return tags;
	}


	/**
	 * @return the instances
	 */
	public ArrayList<Instance> getInstances() {
		return instances;
	}


	/**
	 * @return the attributes
	 */
	public ArrayList<Attribute> getAttributes() {
		ArrayList<Attribute> attr = new ArrayList<Attribute>();
		attr.addAll(attributes.values());
		return attr;
	}
	
	public Instance addInstance(){
		Instance instance = new Instance(this);
		this.instances.add(instance);
		return instance;
	}


	/**
	 * @return the instanceNamespace
	 */
	public ArrayList<String> getInstanceNamespace() {
		return instanceNamespace;
	}
	
	public Attribute getAttributeByName(String name){
		
		for(Attribute attr:this.getAttributes()){
			if(attr.getName().equals(name)){
				return attr;
			}
		}
		
		return null;
		
	}
	
	public void setIdAttributte(Attribute attr,IdAttributeOpts value){
		if(!this.attributes.values().contains(attr)){
			logger.debug("Trying to make "+attr+" idAttribute but this attribute does not exist for this entity");
		}else{
			//test if there is something to be changed
			if(!attr.getIsIdAttribute().equals(value)){
				//If AUTO clear all values
				if(value.equals(IdAttributeOpts.AUTO)){
					for(Instance inst:this.instances){
						inst.removeValue(attr);
					}
					attr.setDataType(EMXDataType.STRING);
				}
				if(value.equals(IdAttributeOpts.TRUE)){
					//Currently not checking for unique values
				}
				attr.setIsIdAttribute(value);
			}
		}
		
		
	}
	
	public boolean changeName(String value){
		
		if(!EMX.isValidEMXName(value)||this.inPackage.namespace.contains(value)){
			return false;
		}else{
			this.inPackage.namespace.remove(this.name);
			this.inPackage.namespace.add(value);
			this.name = value;
			return true;
		}
		
	}



}
