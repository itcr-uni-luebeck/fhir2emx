/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.InvalidNameException;


public class EMX extends EMXPackage{
	
	private HashMap<String,Tag> tagNamespace;
	
	/**
	 * All tables to be created / extended in the MOLGENIS instance in tsv formatted strings
	 */
	private HashMap<String, String> tables;
	

	private server.MOLGENISServerConnector serverNameChecker;
	
	/**
	 * @param modelName
	 * @throws InvalidNameException
	 */
	public EMX(String modelName) throws InvalidNameException{
		super(modelName);
		this.tables = new HashMap<String,String>();
		this.namespace = new ArrayList<String>();
		this.entities = new ArrayList<Entity>();
		this.subPackages = new ArrayList<EMXPackage>();
		this.tagNamespace = new HashMap<String,Tag>();
	}
	
	
	
	public void setServerNameChecker(server.MOLGENISServerConnector checker){
		this.serverNameChecker = checker;
	}
	
	
	public static boolean isValidEMXName(String name){
		
		//Test length
		if(name.length() > 30){
			return false;
		}
		//Test full name
		if(!name.matches("^[a-zA-Z][a-zA-Z0-9#_]*$")){
			return false;
		}
		
		//Spilt into single Package/Entity names
		String[] names = name.split("_");
		
		//Test subnames again, no _ allowed!
		for(String subname:names){
			if(!subname.matches("^[a-zA-Z][a-zA-Z0-9#]*$")){
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Parses the current model state into EMX tables. The generated tables can
	 * be retrieved with getTables();
	 */
	public void generateTables(boolean withTags) {

		String delimiter = "\t";
		String newline = "\n";

		if (withTags) {
			// ->Tags table - web interface says everything but codeSystem and
			// ObjectIRI is mandatory but doku says only identifier and name
			// are...
			// Write header containing ALL columns
			String tagTable = "identifier" + delimiter + "label" + delimiter + "objectIRI" + delimiter + "relationLabel"
					+ delimiter + "codeSystem" + delimiter + "relationIRI";
			// Finding all tags is easy since there is a static list in the Tag
			// class
			for (Tag tag : this.tagNamespace.values()) {
				tagTable = tagTable.concat(newline + tag.getIdentifier() + delimiter + tag.getLabel() + delimiter
						+ tag.getObjectIRIAsString() + delimiter + tag.getRelationLabel() + delimiter
						+ tag.getCodeSystem() + delimiter + tag.getRelationIRIAsString());
			}

			this.tables.put("tags", tagTable);
		}
		// ->Packages table
		// First collect all Packages in List
		List<EMXPackage> packages = new ArrayList<EMXPackage>();
		packages.add(this);
		// Expand Package list
		packages.addAll(this.getExpandedSubPackagesList());

		String packagesTable = "name" + delimiter + "description" + delimiter + "parent";
		if (withTags) {
			packagesTable = packagesTable + delimiter + "tags";
		}

		for (EMXPackage currPackage : packages) {
			String parentName = "";
			if (!(currPackage.getParent() == null)) {
				parentName = currPackage.getParent().getFullName();
			}
			packagesTable = packagesTable.concat(newline + currPackage.getFullName() + delimiter
					+ currPackage.getDescription() + delimiter + parentName + delimiter);
			if (withTags) {
				// Add all tags
				String tags = "";
				for (Tag t : currPackage.getTags()) {
					tags = tags.concat(t.getIdentifier() + ",");
				}
				if (!tags.isEmpty()) {
					// Remove last comma
					tags = tags.substring(0, tags.length() - 1);
				}
				packagesTable = packagesTable.concat(tags);
			}
		}

		this.tables.put("packages", packagesTable);

		// -> enities table
		String entitiesTable = "name" + delimiter + "extends" + delimiter + "package" + delimiter + "abstract"
				+ delimiter + "description";
		// Add fields for all language descriptions
		for (EMXLangCode code : EMXLangCode.values()) {
			entitiesTable = entitiesTable.concat(delimiter + "description-" + code.toString());
		}
		if (withTags) {
			entitiesTable = entitiesTable.concat(delimiter + "tags");
		}
		// Get all entities
		List<Entity> entities = new ArrayList<Entity>();
		for (EMXPackage currPackage : packages) {
			entities.addAll(currPackage.getEntities());
		}

		for (Entity entity : entities) {
			String entityLine = "";
			String extendsEntityName = "";
			if (!(entity.getExtendsEntity() == null)) {
				extendsEntityName = entity.getExtendsEntity().getFullName();
			}
			entityLine = entityLine.concat(newline + entity.getName() + delimiter + extendsEntityName + delimiter
					+ entity.getInPackage().getFullName() + delimiter
					+ new Boolean(entity.isAbstract()).toString().toUpperCase() + delimiter + entity.getDescription());
			// Add all language descriptions
			for (EMXLangCode code : EMXLangCode.values()) {
				entityLine = entityLine.concat(delimiter + entity.getDescription(code));
			}
			if (withTags) {
				// Add Tags
				entityLine = entityLine.concat(delimiter);
				for (Tag tag : entity.getTags()) {
					entityLine = entityLine.concat(tag.getIdentifier() + ",");
				}
				// Remove last comma
				entityLine = entityLine.substring(0, entityLine.length() - 1);
			}
			// Add instance line to entities table
			entitiesTable = entitiesTable.concat(entityLine);
		}

		this.tables.put("entities", entitiesTable);

		// ->Attributes table

		String attributesTable = "name" + delimiter + "entity" + delimiter + "dataType" + delimiter + "refEntity"
				+ delimiter + "nillable" + delimiter + "idAttribute" + delimiter + "description";
		// Add fields for all language descriptions
		for (EMXLangCode code : EMXLangCode.values()) {
			attributesTable = attributesTable.concat(delimiter + "description-" + code.toString());
		}
		attributesTable = attributesTable.concat(
				delimiter + "rangeMin" + delimiter + "rangeMax" + delimiter + "lookupAttribute" + delimiter + "label");
		// Add fields for all language labels
		for (EMXLangCode code : EMXLangCode.values()) {
			attributesTable = attributesTable.concat(delimiter + "label-" + code.toString());
		}
		attributesTable = attributesTable
				.concat(delimiter + "aggregateable" + delimiter + "labelAttribute" + delimiter + "readOnly");
		if (withTags) {
			attributesTable = attributesTable.concat(delimiter + "tags");
		}
		attributesTable = attributesTable.concat(delimiter + "validationExpression" + delimiter + "defaultValue"
				+ delimiter + "partOfAttribute" + delimiter + "expression" + delimiter + "enumOptions");

		// Get List of all attributes

		List<Attribute> attributes = new ArrayList<Attribute>();

		for (Entity entity : entities) {
			attributes.addAll(entity.getAttributes());
		}

		// Add line for each attribute
		for (Attribute attr : attributes) {
			String refEntityName = "";
			if (!(attr.getRefEntity() == null)) {
				refEntityName = attr.getRefEntity().getFullName();
			}
			String attrLine = newline + attr.getName() + delimiter + attr.getEntity().getFullName() + delimiter
					+ attr.getDataType().toString() + delimiter + refEntityName + delimiter
					+ new Boolean(attr.isNillable()).toString().toUpperCase() + delimiter
					+ attr.getIsIdAttribute().toString() + delimiter + attr.getDescription();
			// Add fields for all language descriptions
			for (EMXLangCode code : EMXLangCode.values()) {
				attrLine = attrLine.concat(delimiter + attr.getDescription(code));
			}
			attrLine = attrLine.concat(delimiter + attr.getRangeMinAsString() + delimiter + attr.getRangeMaxAsString()
					+ delimiter + new Boolean(attr.isLookupAttribute()).toString().toUpperCase() + delimiter
					+ attr.getLabel());
			// Add fields for all language labels
			for (EMXLangCode code : EMXLangCode.values()) {
				attrLine = attrLine.concat(delimiter + attr.getLabel(code));
			}
			attrLine = attrLine.concat(delimiter + new Boolean(attr.isAggregateable()).toString().toUpperCase()
					+ delimiter + new Boolean(attr.isLabelAttribute()).toString().toUpperCase() + delimiter
					+ new Boolean(attr.isReadOnly()).toString().toUpperCase());
			if (withTags) {
				// Add Tags
				attrLine = attrLine.concat(delimiter);
				for (Tag tag : attr.getTags()) {
					attrLine = attrLine.concat(tag.getIdentifier() + ",");
				}
				// Remove last comma
				attrLine = attrLine.substring(0, attrLine.length() - 1);
			}
			String partOfName = "";
			if (!(attr.getPartOfAttribute() == null)) {
				partOfName = attr.getPartOfAttribute().getName();
			}
			attrLine = attrLine
					.concat(delimiter + attr.getValidationExpression() + delimiter + attr.getDefaultValue() + delimiter
							+ partOfName + delimiter + attr.getExpression() + delimiter + attr.getEnumOptionsAsCSV());

			attributesTable = attributesTable.concat(attrLine);
		}

		this.tables.put("attributes", attributesTable);

		// ->Tables for each entity (with Instances)

		for (Entity entity : entities) {
			// Write header row
			// Enter first element seperate because of no delimiter at the
			// start
			if (!entity.getAttributes().isEmpty()) {
				String entityTable = entity.getAttributes().get(0).getName();
				for (int i = 1; i < entity.getAttributes().size(); i++) {
					Attribute attr = entity.getAttributes().get(i);
					entityTable = entityTable.concat(delimiter + attr.getName());
				}

				// Add Instance Rows
				for (Instance inst : entity.getInstances()) {
					// Add first element separately because of no delimiter at
					// start
					String instLine = newline + inst.getValue(entity.getAttributes().get(0));
					// Fill non existent instance attributes with "" to avoid
					// NullPointerException
					inst.fillAllAttributes();
					for (int j = 1; j < entity.getAttributes().size(); j++) {
						Attribute attr = entity.getAttributes().get(j);
						instLine = instLine.concat(delimiter + inst.getValue(attr));
					}
					entityTable = entityTable.concat(instLine);
				}

				this.tables.put(entity.getFullName(), entityTable);
			}
		}
	}
	
	@Override
	protected server.MOLGENISServerConnector getServerNameChecker(){
			return this.serverNameChecker;
	}
	
	public void setRootPackageName(String rootName) throws InvalidNameException{
		if(isValidEMXName(rootName)){
			this.name = rootName;
		}else{
			throw new InvalidNameException(rootName+" is no valid EMX name!");
		}
	}




	/**
	 * @return the tables
	 */
	public HashMap<String, String> getTables(boolean withTags) {
		if(this.tables.isEmpty()){
			this.generateTables(withTags);
		}
		return tables;
	}
	
	HashMap<String,Tag> getTagNamespace(){
		return this.tagNamespace;
	}
	
	public boolean tagExists(String identifier){
		
		return this.tagNamespace.containsKey(identifier);
		
	}
	
	public Tag getTag(String identifier){
		
		return this.tagNamespace.get(identifier);
		
	}
	
	public boolean isTagNameAvailable(String name){
		if(!EMX.isValidEMXName(name)){
			return false;
		}
		if(tagNamespace.containsKey(name)){
			return false;
		}
		
		return true;
	}
	
	public Tag removeTag(String identifier){
		return tagNamespace.remove(identifier);
	}
	
	
	
}
