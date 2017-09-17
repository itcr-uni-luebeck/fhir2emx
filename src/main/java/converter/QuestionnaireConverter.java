/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemOptionComponent;
import org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.dstu3.model.ValueSet.ValueSetExpansionContainsComponent;
import org.hl7.fhir.dstu3.model.codesystems.IssueSeverity;
import org.hl7.fhir.exceptions.FHIRException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import emxModel.Attribute;
import emxModel.AttributeNonExistentException;
import emxModel.EMX;
import emxModel.EMXDataType;
import emxModel.Entity;
import emxModel.IdAttributeOpts;
import emxModel.Instance;
import emxModel.Tag;
import server.FHIRResourceFetcher;
import server.MOLGENISServerConnector;


public class QuestionnaireConverter {
	
	private EMX emx;

	private Questionnaire questionnaire;
	
	private ConversionOutcome outcome;
	
	private FHIRResourceFetcher fetcher;
	
	private MOLGENISServerConnector connector;
	
	private static Map<QuestionnaireItemType,EMXDataType> typeMapping;
	
	final static Logger logger = LoggerFactory.getLogger(QuestionnaireConverter.class);
	
	
	public QuestionnaireConverter(Questionnaire questionnaire, FHIRResourceFetcher fetcher, MOLGENISServerConnector connector) {

		this.fetcher = fetcher;
		this.connector = connector;

		// Populate Map once
		if (typeMapping == null) {
			typeMapping = new HashMap<QuestionnaireItemType, EMXDataType>();
			typeMapping.put(QuestionnaireItemType.GROUP, EMXDataType.COMPOUND);
			typeMapping.put(QuestionnaireItemType.BOOLEAN, EMXDataType.BOOL);
			typeMapping.put(QuestionnaireItemType.DECIMAL, EMXDataType.DECIMAL);
			typeMapping.put(QuestionnaireItemType.INTEGER, EMXDataType.INT);
			typeMapping.put(QuestionnaireItemType.DATE, EMXDataType.DATE);
			typeMapping.put(QuestionnaireItemType.DATETIME, EMXDataType.DATETIME);
			typeMapping.put(QuestionnaireItemType.TIME, EMXDataType.DATETIME);
			typeMapping.put(QuestionnaireItemType.STRING, EMXDataType.STRING);
			typeMapping.put(QuestionnaireItemType.TEXT, EMXDataType.TEXT);
			typeMapping.put(QuestionnaireItemType.URL, EMXDataType.HYPERLINK);
			typeMapping.put(QuestionnaireItemType.CHOICE, EMXDataType.CATEGORICAL);
			typeMapping.put(QuestionnaireItemType.OPENCHOICE, EMXDataType.CATEGORICAL);
			typeMapping.put(QuestionnaireItemType.ATTACHMENT, EMXDataType.FILE);
			
			typeMapping.put(QuestionnaireItemType.REFERENCE, EMXDataType.STRING);
			typeMapping.put(QuestionnaireItemType.QUANTITY, EMXDataType.STRING);
		}
		try {
			this.emx = new emxModel.EMX("fhir");
			if(this.hasMOLGENISConnection()){
				this.emx.setServerNameChecker(connector);
			}
			
		} catch (InvalidNameException e) {
			// Should never happen because name is fixed to valid value
			e.printStackTrace();
		}
		this.questionnaire = questionnaire;
		this.outcome = new ConversionOutcome();

	}


	public void convert() {
		if(this.questionnaire == null){
			return;
		}
		// Start conversion by generating EMX Identifier
		//For some reason URL is 0..1, catch no URL
		String emxEntityName = null;
		if(this.questionnaire.getUrl() != null && !this.questionnaire.getUrl().isEmpty()){
			emxEntityName = QuestionnaireConverter.generateEMXIDFromInvalidString(this.questionnaire.getUrl());
		}else{
			emxEntityName = QuestionnaireConverter.generateEMXIDFromInvalidString(this.questionnaire.getId());
		}
		

		// Rename root Package to pk+"emxidentifier" e.g."pk1234567890"
		try {
			this.emx.setRootPackageName("pk" + emxEntityName);
		} catch (InvalidNameException e) {
			// Should never happen because pk+11 chars is valid
			e.printStackTrace();
			// ..but just in case
			logger.debug("Error generating root package name. It will now be called "+this.emx.getName(),e);
			this.outcome.addError(IssueSeverity.WARNING,"emx.name","Error generating root package name. It will now be called "+this.emx.getName());
		}
		this.emx.setDescription(
				"Package for all entities generated from the FHIR Questionnaire " + this.questionnaire.getUrl());


		// Add Main entity for Questionnaire
		Entity mainEntity = null;
		try {
			mainEntity = this.emx.addEntity(emxEntityName);
		} catch (NamingException e) {
			// Only happens when name is already taken on Server...
			logger.error("Unable to add main Entity. Aborted conversion",e);
			this.outcome.addError(IssueSeverity.FATAL, emxEntityName, "Unable to add main Entity. Aborted conversion");
			e.printStackTrace();
			// ..but if it does there's nothing we can do.
			return;
		}

		mainEntity.setDescription(this.questionnaire.getDescription());

		// Add Tag with FHIR URL
		try {
			mainEntity.addTag("URLto" + emxEntityName, this.questionnaire.getUrl());
		} catch (NamingException e) {
			// Again, this should not happen. If it does, without Tag the
			// corresponding Questionnaire can't be identified.
			logger.debug("Unable to add URL Tag",e);
			this.outcome.addError(IssueSeverity.WARNING, emxEntityName, "Unable to add URL Tag");
			e.printStackTrace();
		}
		
		//Add ID Attribute
		try {
			Attribute idAttr = mainEntity.addAttribute("id");
			mainEntity.setIdAttributte(idAttr, IdAttributeOpts.AUTO);
		} catch (NamingException e) {
			logger.error("Failed to add ID Attribute to "+mainEntity.getFullName()+". Aborting conversion..");
			this.outcome.addError(IssueSeverity.ERROR, mainEntity.getFullName(), "Failed to add ID Attribute. Generated EMX will be invalid");
			e.printStackTrace();
		}

		// Now the real work begins... parse the items!
		// Iterate over first level items, sublevel items will be parsed by
		// recursive calls
		for (QuestionnaireItemComponent item : this.questionnaire.getItem()) {
			this.parseItem(item, mainEntity, null);
		}
		
	}

	
	
	/**
	 * Parses Single Questionnaire item
	 * @param item The item to be parsed
	 * @param entity The Entity to add the parsed Attribute to
	 * @param parentAttribute Attribute to use in partOfAttribute of the parsed Attribute (may be null)
	 */
	private void parseItem(QuestionnaireItemComponent item, Entity entity, Attribute parentAttribute) {


		// Skip display items
		if (item.getType().equals(QuestionnaireItemType.DISPLAY)) {
			return;
		}

		// Add attribute for this Item
		Attribute attr = null;
		try {
			attr = entity.addAttribute(item.getLinkId());
		} catch (NamingException e) {
			// transform into valid emx... if this becomes necessary, question
			// reidentification becomes hard because linkID is lost forever
			try {
				attr = entity.addAttribute(QuestionnaireConverter.generateEMXIDFromInvalidString(item.getLinkId()));
			} catch (NamingException e1) {
				// This should definitely not happen
				e1.printStackTrace();
			}
		}
		
		
		//parse definition if existent
		if(!(item.getDefinition() == null)&&(!item.getDefinition().isEmpty())){
			item = parseElementdefinitionIntoEmptyItemFields(item);
		}
		
		if(parentAttribute != null){
			attr.setPartOfAttribute(parentAttribute);
		}

		
		if(!(item.getPrefix() == null)){
			attr.setLabel(item.getPrefix());
		}
		if(!(item.getText() == null)){
			attr.setDescription(item.getText());
		}
		attr.setReadOnly(item.getReadOnly());
		attr.setDataType(typeMapping.get(item.getType()));
		
		//Parse codes
		attr = this.addCodesAsTagsToAttribute(item.getCode(), attr);

		//Parse Option(s)
		if (item.getType().equals(QuestionnaireItemType.CHOICE)
				|| item.getType().equals(QuestionnaireItemType.OPENCHOICE)) {
			//Prepare Entity
			// Generate new Entity for Options (in RootPackage)
			// Create Entity
			String entityName = attr.getName() + "Enum";
			Entity codeListEntity = null;
			try {
				codeListEntity = this.emx.addEntity(entityName);
			} catch (NamingException e) {
				try {
					codeListEntity = this.emx
							.addEntity(QuestionnaireConverter.generateEMXIDFromInvalidString(entityName));
				} catch (NamingException e1) {
					// Definitely not happening
					e1.printStackTrace();
				}
			}
			codeListEntity.setDescription("Contains all possible values for" + attr.getName());
			//Set reference in main entity attribute
			attr.setRefEntity(codeListEntity);
			attr.setDataType(EMXDataType.CATEGORICAL);
			
			
			//If Options are given in ValueSet
			if (!item.getOptions().isEmpty()) {
				ValueSet valueSet = fetcher.resolveValueSetReference(questionnaire,item.getOptions());
				if(valueSet != null){
					codeListEntity = parseValueSet(valueSet, codeListEntity);
				}else{
					//Resoving Value Set Reference was not possible. Build Attribute instead
					this.emx.removeEntity(codeListEntity);
					attr.setRefEntity(null);
					attr.setDataType(EMXDataType.TEXT);
					attr.setDescription("Contains codes from: ");
					if(item.getOptions().getReference() != null){
						attr.setDescription(attr.getDescription()+(item.getOptions().getReference())+"; ");
					}
					if(item.getOptions().getDisplay() != null){
						attr.setDescription(attr.getDescription()+(item.getOptions().getDisplay())+"; ");
					}
					
				}
				
			} else {
				//If options are given in item
				if (!item.getOption().isEmpty()) {
						codeListEntity = parseOptionList(item.getOption(),codeListEntity);
						
					} else {
						logger.error("Questionnaire has empty option list");
				}
			}
		}

		// If Subitems: build compound
		if(!item.getItem().isEmpty()){
			//Easy case
			if(item.getType().equals(QuestionnaireItemType.GROUP)){
				attr.setDataType(EMXDataType.COMPOUND);
				//Recursive call for all subitems
				for(QuestionnaireItemComponent subItem:item.getItem()){
					this.parseItem(subItem, entity, attr);
				}
			}else{
				//Add second Attribute to use as compound because compound attributes are abstract but this item is not of abstract GROUP type
				Attribute compoundAttr = null;
				try {
					compoundAttr = entity.addAttribute(attr.getName()+"Comp");
				} catch (NamingException e) {
					//Auto-generated catch block
					e.printStackTrace();
				}
				compoundAttr.setDataType(EMXDataType.COMPOUND);
				//enable nesting
				compoundAttr.setPartOfAttribute(attr.getPartOfAttribute());
				//Recursive call for all subitems
				for(QuestionnaireItemComponent subItem:item.getItem()){
					this.parseItem(subItem, entity, compoundAttr);
				}
			}
		}
	}
	
	/**
	 * Parses Value Set into Entity with ValueSet Elements as Instances
	 * @param vs
	 * @return
	 */
	public Entity parseValueSet(ValueSet vs, Entity codeListEntity){
		//Generate some Metadata for ValueSet Description
		if(vs.getUrl() != null){
			codeListEntity.setDescription(codeListEntity.getDescription()+", Instances generated from "+vs.getUrl());
		}
		if(vs.getName() != null){
			codeListEntity.setDescription(codeListEntity.getDescription()+", "+vs.getName());
		}
		if(vs.getTitle() != null){
			codeListEntity.setDescription(codeListEntity.getDescription()+", "+vs.getTitle());
		}
		
		//Only use Expanded values
		codeListEntity.setDescription(codeListEntity.getDescription()+" Based on Expansion "+vs.getExpansion().getIdentifier());
		
		//Prepare Attributes
		Attribute code;
		try {
			code = codeListEntity.addAttribute("code");
			codeListEntity.setIdAttributte(code, IdAttributeOpts.TRUE);
		} catch (NamingException e) {
			e.printStackTrace();
			logger.debug(("Could not create IdAttribute for Code List Entity. Abort."),e);
			this.outcome.addError(IssueSeverity.ERROR, codeListEntity.getName(), "Could not create IdAttribute for Code List Entity. The generated EMX may be invalid");
			return codeListEntity;
		}
		Attribute system= null;
		try {
			system = codeListEntity.addAttribute("system");
		} catch (NamingException e) {
			logger.debug("Could not add System Attribute for Code List Entity. Some Information will be lost.",e);
			this.outcome.addError(IssueSeverity.WARNING, codeListEntity.getName(), "Could not add System Attribute for Code List Entity. Some Information will be lost.");
			e.printStackTrace();
		}
		
		Attribute version = null;
		try {
			version = codeListEntity.addAttribute("version");
		} catch (NamingException e) {
			logger.debug("Could not add Version Attribute for Code List Entity. Some Information will be lost.",e);
			this.outcome.addError(IssueSeverity.WARNING, codeListEntity.getName(), "Could not add Version Attribute for Code List Entity. Some Information will be lost.");
			e.printStackTrace();
		}
		Attribute inactive = null;
		try {
			inactive = codeListEntity.addAttribute("inactive");
			inactive.setDataType(EMXDataType.BOOL);
		} catch (NamingException e) {
			logger.debug("Could not add Inactive Attribute for Code List Entity. Some Information will be lost.",e);
			this.outcome.addError(IssueSeverity.WARNING, codeListEntity.getName(), "Could not add Inactive Attribute for Code List Entity. Some Information will be lost.");
			e.printStackTrace();
		}
		
		Attribute display = null;
		try {
			display = codeListEntity.addAttribute("display");
		} catch (NamingException e) {
			logger.debug("Could not add Display Attribute for Code List Entity. Some Information will be lost.",e);
			this.outcome.addError(IssueSeverity.WARNING, codeListEntity.getName(), "Could not add Display Attribute for Code List Entity. Some Information will be lost.");
			e.printStackTrace();
		}
		
		parseContainsElements(vs.getExpansion().getContains(), codeListEntity, code, system, version, display, inactive);
		
		return codeListEntity;
	}
	
	
	public Entity parseContainsElements(List<ValueSetExpansionContainsComponent> containsList, Entity codeListEntity,
			Attribute code, Attribute system, Attribute version, Attribute display, Attribute inactive) {
		// Parse all Contains Elements
		for (ValueSetExpansionContainsComponent contains : containsList) {
			Instance entry = null;
			if (!(contains.getCode() == null || (contains.hasAbstract() && contains.getAbstract() == true))) {
				entry = codeListEntity.addInstance();
				try {
					entry.addValue(code, contains.getCode());
					entry.addValue(display, contains.getDisplay());
					entry.addValue(system, contains.getVersion());
					entry.addValue(version, contains.getVersion());
					entry.addValue(inactive, Boolean.toString(contains.getInactive()));
				} catch (InvalidAttributeValueException | NameAlreadyBoundException | DataFormatException
						| AttributeNonExistentException e) {
					this.outcome.addError(IssueSeverity.ERROR, codeListEntity.getName(),
							"Failed to add some Attribute information for " + contains.getCode()
									+ " The corresponding Instance will lack some information and may be invalid.");
					e.printStackTrace();
				}
			}
			
			if (contains.getContains() != null && (!contains.getContains().isEmpty())) {
				parseContainsElements(contains.getContains(), codeListEntity, inactive, inactive, inactive, inactive,
						inactive);
			}
		}
		return codeListEntity;
	}
	/**
	 * Parses all "option" elements of the given item into an entity with an instance for each option.
	 * @param options
	 * @param codeListEntity
	 * @return The same Entity given as Param, only with Instances generated from options
	 */
	public Entity parseOptionList(List<QuestionnaireItemOptionComponent> options,Entity codeListEntity){
		//There is always an id attribute
		Attribute idAttribute = null;
		try {
			idAttribute = codeListEntity.addAttribute("identifier");
			codeListEntity.setIdAttributte(idAttribute, IdAttributeOpts.TRUE);
		} catch (NamingException e) {
			logger.debug("Could not generated ID Attribute. The generated EMX will be Invalid.",e);
			this.outcome.addError(IssueSeverity.WARNING, codeListEntity.getFullName(), "Could not generated ID Attribute. The generated EMX will be Invalid.");
			e.printStackTrace();
		}
		
		//Parse Coding Options
		if (options.get(0).hasValueCoding()) {
			//Set up attributes
			Attribute system = null;
			Attribute version = null;
			Attribute display = null;
			Attribute userSelected = null;
			try {
				system = codeListEntity.addAttribute("system");
				version = codeListEntity.addAttribute("version");
				display = codeListEntity.addAttribute("display");
				display.setLabelAttribute(true);
				userSelected = codeListEntity.addAttribute("userSelected");
				userSelected.setDataType(EMXDataType.BOOL);
			} catch (NamingException e) {
				logger.debug("Error during options parsing",e);
			}
			//Add instance for each option
			for (QuestionnaireItemOptionComponent option : options) {
				Instance optInstance = codeListEntity.addInstance();
				try {
					optInstance.addValue(idAttribute, option.getValueCoding().getCode());
					optInstance.addValue(system, option.getValueCoding().getSystem());
					optInstance.addValue(version, option.getValueCoding().getVersion());
					optInstance.addValue(display, option.getValueCoding().getDisplay());
					optInstance.addValue(userSelected, ((Boolean)option.getValueCoding().getUserSelected()).toString());
				} catch (NameAlreadyBoundException | InvalidAttributeValueException | DataFormatException | AttributeNonExistentException | FHIRException e) {
					logger.debug("Error parsing Code List Item "+option.getValue()+"as Coding.",e);
					this.outcome.addError(IssueSeverity.WARNING,codeListEntity.getFullName(),"Error parsing Code List Item "+option.getValue()+"as Coding.");
					e.printStackTrace();
				}
			}
		}else{
			//Parse options as single attribute Instances
			//add instance for each option
			for (QuestionnaireItemOptionComponent option : options) {
				Instance optInstance = codeListEntity.addInstance();
				// All other types can easily be represented as
				// single
				// string
				String valueString = "";
				try {
					// Integer
					if (option.hasValueIntegerType()) {

						valueString = option.getValueIntegerType().toString();

					}
					// date
					if (option.hasValueDateType()) {
						valueString = option.getValueDateType().toHumanDisplayLocalTimezone();
					}
					// time
					if (option.hasValueTimeType()) {
						valueString = option.getValueTimeType().toString();
					}
					// string
					if (option.hasValueStringType()) {
						valueString = option.getValueStringType().toString();
					}
				} catch (FHIRException e) {
					logger.debug("Error parsing Code List Item "+option.getValue()+"as Simple Type.",e);
					this.outcome.addError(IssueSeverity.WARNING,codeListEntity.getFullName(),"Error parsing Code List Item "+option.getValue()+"as Simple Type.");
					e.printStackTrace();
				}
				try {
					optInstance.addValue(idAttribute, valueString);
				} catch (InvalidAttributeValueException | DataFormatException | AttributeNonExistentException | NameAlreadyBoundException e) {
					// Should not happen
					logger.debug("Error setting Attribute to Value "+valueString+" for Code List Item.",e);
					this.outcome.addError(IssueSeverity.WARNING,codeListEntity.getFullName(),"Error setting Attribute to Value "+valueString+" for Code List Item.");
					e.printStackTrace();
				}
			}
		}
		return codeListEntity;
	}
	
	
	/**
	 * Parses some Info (list given in FHIR doc) from the given Items .definition into some of its Fields if they are empty.
	 * According to the FHIR Specification, Values from the Item override those from the Element Definition.
	 * @param item
	 * @return
	 */
	public QuestionnaireItemComponent parseElementdefinitionIntoEmptyItemFields(QuestionnaireItemComponent item){
	
		String uri = item.getDefinition();
		ElementDefinition elemDef = fetcher.resolveElementDefinitionURI(uri);
		
		// Merge code lists
		if (item.hasCode() && elemDef.hasCode()) {
			item.getCode().addAll(elemDef.getCode());
		} else {
			if ((!item.hasCode()) && elemDef.hasCode()) {
				item.setCode(elemDef.getCode());
			}
		}
		
		//required
		if(!item.hasRequired()){
			item.setRequired(elemDef.getMin()>0);
		}
		//repeats
		if(!item.hasRepeats()&&elemDef.getMax() != null){
			item.setRepeats(elemDef.getMax().equals("*")||Integer.parseInt(elemDef.getMax())>1);
		}
		//maxLength
		if(!item.hasMaxLength()){
			item.setMaxLength(elemDef.getMaxLength());
		}
		//options
		if((!item.hasOptions())&&elemDef.hasBinding()){
			try {
				item.setOptions(elemDef.getBinding().getValueSetReference());
			} catch (FHIRException e) {
				e.printStackTrace();
				logger.debug("Attempting to generate Reference from URI...");
				try {
					item.setOptions(new Reference(elemDef.getBinding().getValueSetUriType().asStringValue()));
				} catch (FHIRException e1) {
					//Ok, nothing Worked so carry on... 
					logger.debug("Failed to read elemDef.binding. This will be Ignored.");
					this.outcome.addError(IssueSeverity.WARNING, elemDef.getPath(), "Failed to read elemDef.binding. This will be Ignored.");
					e1.printStackTrace();
				}
			}
		}
		
		return item;
	}
	
	
	private Attribute addCodesAsTagsToAttribute(List<Coding> codes, Attribute attr) {

		for (Coding coding : codes) {
			if (this.emx.tagExists(coding.getCode())) {
				attr.addTag(this.emx.getTag(coding.getCode()));
			} else {
				if (EMX.isValidEMXName(coding.getCode())) {
					try {
						Tag tag = attr.addTag(coding.getCode(), coding.getDisplay());
						tag.setCodeSystem(coding.getSystem());
					} catch (NamingException e) {
						// We will just skip this code for now...
						logger.debug("Failed to add Code " + coding.getCode(),e);
						this.outcome.addError(IssueSeverity.WARNING, attr.getName(),
								"Failed to add Code " + coding.getCode());
						e.printStackTrace();
					}
				}else{
					logger.debug("Failed to add Code " + coding.getCode() + " because it is not a valid EMX identifier");
					this.outcome.addError(IssueSeverity.WARNING, attr.getName(),
							"Failed to add Code " + coding.getCode() + " because it is not a valid EMX identifier");
				}
			}
		}

		return attr;
	}
	
	public ConversionOutcome getOutcome(boolean withTags) {
		if(this.emx.getEntities().isEmpty()){
			ConversionOutcome emptyEMX = new ConversionOutcome();
			emptyEMX.addError(IssueSeverity.FATAL, "Questionnaire", "Could not generate any Entity");
			return emptyEMX;
		}
		this.outcome.setEmxTables(this.emx.getTables(withTags));
		return this.outcome;
	}
	
	/**
	 * Mainly for Test Purposes, use with Caution!
	 * @return
	 */
	public EMX getEmx(){
		return this.emx;
	}
	
	private boolean hasMOLGENISConnection(){
		return this.connector != null;
	}


	/**
	 * According to Java spec: "As much as is reasonably practical, the hashCode method defined by class Object does return distinct integers for distinct objects."
	 * So, should return distinct values for distinct URLs.
	 * CAUTION: Hash is not reversible (may work different on different machines), so hash->URL not possible! Instead save URL via EMX Tag
	 * @param identifier, usually FHIR Resource URL
	 * @return String of length 11 containing numbers and Leading with Letter 'A'(valid EMX identifier)
	 */
	
	public static String generateEMXIDFromInvalidString(String identifier){
		
		String emxId = 'A'+Integer.toUnsignedString(identifier.hashCode());
		
		
		return emxId;
		
	}

}
