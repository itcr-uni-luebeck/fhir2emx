/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package server;

import org.hl7.fhir.dstu3.model.DataElement;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import converter.QuestionnaireConverter;

public class FHIRResourceFetcher {
	
	private  FhirContext context;
	
	private String baseURL;
	
	final static Logger logger = LoggerFactory.getLogger(QuestionnaireConverter.class);
	

	/**
	 * @param context
	 * @param baseURL
	 * @param token
	 */
	public FHIRResourceFetcher(FhirContext context, String baseURL){
		
		this.baseURL = baseURL;
		this.context = context;
		
		
	}
	
	public Questionnaire fetchQuestionnaire(String questionnaireID){
		Questionnaire ques = null;
		try{
			ques = context.newRestfulGenericClient(baseURL).read().resource(Questionnaire.class).withId(questionnaireID).execute();
		}catch(Exception e){
			logger.error("Error while trying to fetch Questionnaire",e);
			return null;
		}
		return ques;
		
	}
	
	
	/**
	 * Trys to fetch ValueSet. If result contains no expansion, method requestValueSetExpansion is called. If this method returns null (Expansion not possible), this Method will return null too.
	 * @param ques
	 * @param reference
	 * @return Either an expanded valueSet or null
	 */
	public ValueSet resolveValueSetReference(Questionnaire ques, Reference reference){
		
		//Case 1: Contained Resource
		if((reference.getReferenceElement().isLocal())&&(!ques.getContained().isEmpty())){
			for(Resource res:ques.getContained()){
				if(reference.getReferenceElement().getIdPart().equals(res.getId())&&res.getResourceType().equals(ValueSet.class)){
					ValueSet valueSet = (ValueSet) res;
					if(valueSet.hasExpansion()){
						return valueSet;
					}else{
						//Try to expand ValueSet
						return this.requestValueSetExpansion(valueSet);
					}
					
				}
			}
		}
		
		//Case 2: Absolute URL
		if(reference.getReferenceElement().isAbsolute()){
			try{
				ValueSet valueSet = context.newRestfulGenericClient(baseURL).read().resource(ValueSet.class).withUrl(reference.getReference()).execute();
				if(valueSet.hasExpansion()){
					return valueSet;
				}else{
					//Try to expand ValueSet
					return this.requestValueSetExpansion(valueSet);
				}
			}catch(Exception e){
				logger.debug("Unable to resolve referenced resource",e);
				//DADADADUM This is possibly an abstract URL or the Server is offline or or... Just carry on and deal with this in case 4!
			}
		}
		
		//Case 3: Internal URL
		if((!reference.getReferenceElement().isAbsolute())&&(!reference.getReferenceElement().isLocal())&&(!reference.getReferenceElement().isEmpty())){
			try{
				ValueSet valueSet = context.newRestfulGenericClient(baseURL).read().resource(ValueSet.class).withId(reference.getReferenceElement()).execute();
				if(valueSet.hasExpansion()){
					return valueSet;
				}else{
					//Try to expand ValueSet
					return this.requestValueSetExpansion(valueSet);
				}
			}catch(Exception e){
				//So did not work for some Reason. No Problem, continue with case 4!
			}
		}
		
		//Case 4: Abstract (non-resolvable) URL
		
		return null;
	}
	
	
	private ValueSet requestValueSetExpansion(ValueSet valueSet){
		IGenericClient client = this.context.newRestfulGenericClient(baseURL);
		client.registerInterceptor(new LoggingInterceptor(true));
		 
		// Invoke $expand on ValueSet
		Parameters outParams = client
		   .operation()
		   .onInstance(valueSet.getIdElement())
		   .named("$expand")
		   .withNoParameters(Parameters.class) // No input parameters
		   .useHttpGet()
		   .execute();
		
		try{
			ValueSet expanded = (ValueSet) outParams.getParameterFirstRep().getResource();
			return expanded;
		}catch(ClassCastException e){
			e.printStackTrace();
			//Result is probably OperationOutcome, but we don't care here because Expansion did not work anyway.
			return null;
		}
	}
	
	public ElementDefinition resolveElementDefinitionURI(String uri){
		ElementDefinition elemDef = null;
		//Separate the Reference to the containing Resource from the internal reference to the actual Element Definition
		String[] parts = uri.split("#");
		if(parts.length == 1){
			//Assume Resource URL, use first occurrence off Element Definition
			IdType resource = new IdType(parts[0]);
			//For absolute URL
			if(resource.isAbsolute()){
				if(parts[0].contains("DataElement")){
					DataElement element = context.newRestfulGenericClient(baseURL).read().resource(DataElement.class).withUrl(parts[0]).execute();
					elemDef = element.getElementFirstRep();
					return elemDef;
				}else{
					if(parts[0].contains("StructureDefinition")){
						StructureDefinition definition = context.newRestfulGenericClient(baseURL).read().resource(StructureDefinition.class).withUrl(parts[0]).execute();
						elemDef = definition.getSnapshot().getElementFirstRep();
						return elemDef;
					}
				}
				
			}
			//For internal URL
			else{
				if(parts[0].contains("DataElement")){
					DataElement element = context.newRestfulGenericClient(baseURL).read().resource(DataElement.class).withId(resource).execute();
					elemDef = element.getElementFirstRep();
					return elemDef;
				}else{
					if(parts[0].contains("StructureDefinition")){
						StructureDefinition definition = context.newRestfulGenericClient(baseURL).read().resource(StructureDefinition.class).withId(resource).execute();
						elemDef = definition.getSnapshot().getElementFirstRep();
						return elemDef;
					}
				}
				
			}
		}
		if(parts.length == 2){
			//Assume 0 => Resource url, 1 => Element Definition path
			IdType resource = new IdType(parts[0]);
			//For absolute url
			if(resource.isAbsolute()){
				if(parts[0].contains("DataElement")){
					DataElement element = context.newRestfulGenericClient(baseURL).read().resource(DataElement.class).withUrl(parts[0]).execute();
					for(ElementDefinition def: element.getElement()){
						//Look for Element Definition with path matching our param after #
						if(def.getPath().equals(parts[1])){
							//Found it!
							elemDef = def;
							return elemDef;
						}
					}
				}else{
					if(parts[0].contains("StructureDefinition")){
						StructureDefinition definition = context.newRestfulGenericClient(baseURL).read().resource(StructureDefinition.class).withUrl(parts[0]).execute();
						for(ElementDefinition def: definition.getSnapshot().getElement()){
							//Look for Element Definition with path matching our param after #
							if(def.getPath().equals(parts[1])){
								//Found it!
								elemDef = def;
								return elemDef;
							}
						}
					}
				}
				
			}
			//For internal URL
			else{
				if(parts[0].contains("DataElement")){
					DataElement element = context.newRestfulGenericClient(baseURL).read().resource(DataElement.class).withId(resource).execute();
					for(ElementDefinition def: element.getElement()){
						//Look for Element Definition with path matching our param after #
						if(def.getPath().equals(parts[1])){
							//Found it!
							elemDef = def;
							return elemDef;
						}
					}
				}else{
					if(parts[0].contains("StructureDefinition")){
						StructureDefinition definition = context.newRestfulGenericClient(baseURL).read().resource(StructureDefinition.class).withId(resource).execute();
						for(ElementDefinition def: definition.getSnapshot().getElement()){
							//Look for Element Definition with path matching our param after #
							if(def.getPath().equals(parts[1])){
								//Found it!
								elemDef = def;
								return elemDef;
							}
						}
					}
				}
				
			}
		}

		return elemDef;
	}
}
