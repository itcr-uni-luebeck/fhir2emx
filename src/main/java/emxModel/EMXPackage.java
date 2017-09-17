/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converter.QuestionnaireConverter;

public class EMXPackage {
	
	protected String name;
	
	private String description;
	
	private EMXPackage parent;
	
	private ArrayList<Tag> tags;
	
	protected ArrayList<EMXPackage> subPackages;
	
	protected ArrayList<Entity> entities;
	
	protected ArrayList<String> namespace;
	
	final static Logger logger = LoggerFactory.getLogger(QuestionnaireConverter.class);
	
	EMXPackage(String name) throws InvalidNameException{
		
		if(!EMX.isValidEMXName(name)){
			throw new InvalidNameException(name+" is no valid EMX name");
		}
		this.name = name;
		
		this.tags = new ArrayList<Tag>();
		this.subPackages = new ArrayList<EMXPackage>();
		this.entities = new ArrayList<Entity>();
		this.namespace = new ArrayList<String>();
		
	}

	public Entity addEntity(String name) throws NamingException{
		Entity entity = new Entity(name);
		if(this.namespace.contains(name)){
			throw new NameAlreadyBoundException("The name "+name+" already exists in package "+this.getFullName());
		}
		//MOLGENIS name checking
		server.MOLGENISServerConnector connector = this.getServerNameChecker();
		if(connector != null){
			try {
				if(connector.entityExistsOnServer(name)){
					throw new NamingException("The entity "+name+" already exists on specified MOLGENIS Server!");
				}
			} catch (IOException e) {
				logger.debug("Encountered IO Exception while trying to connect to MOLGENIS",e);
			}
		}
		
		entity.setInPackage(this);
		//Throws exception if full name is too long
		if(!EMX.isValidEMXName(entity.getFullName())){
			throw new InvalidNameException("The expanded name: "+entity.getFullName()+" is no vaild EMX name");
		}
		this.namespace.add(name);
		this.entities.add(entity);
		return entity;
	}
	
	
	public EMXPackage addPackage(String name) throws NamingException{
		EMXPackage p = new EMXPackage(name);
		if(this.namespace.contains(name)){
			throw new NameAlreadyBoundException("The name "+name+" already exists in package "+this.getFullName());
		}
		p.setParent(this);
		//Throws exception if full name is too long
		if(!EMX.isValidEMXName(p.getFullName())){
			throw new InvalidNameException("The expanded name: "+p.getFullName()+" is no vaild EMX name");
		}
		this.subPackages.add(p);
		this.namespace.add(name);
		return p;
	}
	
	
	
	public Tag addTag(Tag t){
		this.tags.add(t);
		return t;
	}
	
	

	public Tag addTag(String identifier, String label) throws NamingException{
		return this.addTag(new Tag(identifier,label,this.getRoot().getTagNamespace()));
	}
	
	
	public String getFullName(){
		if(this.parent == null){
			return this.name;
		}
		return this.parent.getFullName()+"_"+this.name;
	}
	
	
	public boolean isNameAvailable(String name){
		if(!EMX.isValidEMXName(this.getFullName()+"_"+name)){
			return false;
		}
		if(this.namespace.contains(name)){
			return false;
		}
		
		return true;
	}

	
	public String getName() {
		return name;
	}

	
	public String getDescription() {
		if(this.description == null){
			return "";
		}
		return description;
	}

	
	public void setDescription(String description) {
		this.description = description;
	}

	
	public EMXPackage getParent() {
		return parent;
	}

	/**
	 * Do not use unless you know exactly what your doing!
	 * @param parent
	 */
	void setParent(EMXPackage parent) {
		this.parent = parent;
	}
	
	
	public List<EMXPackage> getSubpackages(){
		
		return this.subPackages;
	}
	
	
	public EMXPackage removeSubpackage(EMXPackage toBeRemoved){
		this.subPackages.remove(toBeRemoved);
		toBeRemoved.setParent(null);
		this.namespace.remove(toBeRemoved.getName());
		return toBeRemoved;
	}
	
	
	public EMXPackage removeSubpackage(String toBeRemovedPackageName){
		EMXPackage toBeRemoved = null;
		for(EMXPackage p : this.subPackages){
			if(p.getName().equals(toBeRemovedPackageName)||p.getFullName().equals(toBeRemovedPackageName)){
				toBeRemoved = p;
				break;
			}
		}
		return this.removeSubpackage(toBeRemoved);
	}
	
	
	public List<EMXPackage> getExpandedParentList(){
		
		List<EMXPackage> expandedParentList = new ArrayList<EMXPackage>();
		
		if(this.parent != null){
			
			expandedParentList.add(this.parent);
			expandedParentList.addAll(this.parent.getExpandedParentList());

		}
		
		return expandedParentList;
	}
	
	
	public List<EMXPackage> getExpandedSubPackagesList(){
		
		List<EMXPackage> expandedSubPackagesList = new ArrayList<EMXPackage>();
		
		expandedSubPackagesList.addAll(this.subPackages);
		
		for(EMXPackage subPackage: this.subPackages){
			expandedSubPackagesList.addAll(subPackage.getExpandedSubPackagesList());
		}
		
		return expandedSubPackagesList;
	}
	
	server.MOLGENISServerConnector getServerNameChecker(){
		
		return this.parent.getServerNameChecker();
		
	}

	/**
	 * @return the tags
	 */
	public ArrayList<Tag> getTags() {
		return tags;
	}

	/**
	 * @return the entities
	 */
	public ArrayList<Entity> getEntities() {
		return entities;
	}
	
	/**
	 * Returns subpackage with specified name if exists. If not returns null
	 * @param name : The name of the package wanted
	 * @return package with name that equals parameter, or null
	 */
	public EMXPackage getPackageByName(String name){
		
		for(EMXPackage pack:this.getSubpackages()){
			if(pack.name.equals(name)){
				return pack;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns entity with specified name if exists in this package. If not returns null
	 * @param name : The name of the entity wanted
	 * @return entity with name that equals parameter, or null
	 */
	public Entity getEntityByName(String name){
		
		for(Entity entity:this.getEntities()){
			if(entity.getName().equals(name)){
				return entity;
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the Root Package (EMX Object) this Package belongs to.
	 * @return
	 */
	public EMX getRoot(){
		if(this.parent == null){
			return (EMX) this;
		}else{
			return this.parent.getRoot();
		}
	}
	
	public Entity removeEntity(Entity entity){
		this.entities.remove(entity);
		this.namespace.remove(entity.getName());
		return entity;
	}
	
	

}
