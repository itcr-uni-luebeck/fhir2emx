/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package emxModel;

import java.util.HashMap;
import java.util.zip.DataFormatException;

import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.InvalidAttributeValueException;

public class Instance {
	
	private Entity entity;
	
	private HashMap<Attribute,String> values;
	
	public Instance(Entity entity){
		this.entity = entity;
		this.values = new HashMap<Attribute,String>();
	}


	public void addValue(Attribute attr, String value) throws DataFormatException, AttributeNonExistentException,
			InvalidAttributeValueException, NameAlreadyBoundException {
		if(attr == null){
			throw new AttributeNonExistentException("Attribute must not be null");
		}
		//test DataType Format
		if (!Attribute.testDataTypeConformance(attr.getDataType(), value)) {
			throw new DataFormatException(value + " is not of DataType " + attr.getDataType().toString());
		}
		//No values allowed for AUTO id Attributes
		if (attr.getIsIdAttribute().equals(IdAttributeOpts.AUTO)) {
			throw new InvalidAttributeValueException("Values for AUTO idAttributes are not permitted!");
		}
		//Test if entity contains this attribute
		if (entity.getAttributes().contains(attr)) {
			//If this is id Attribute, check for value uniqueness
			if (attr.getIsIdAttribute().equals(IdAttributeOpts.TRUE)) {
				if (this.entity.getInstanceNamespace().contains(value)) {
					throw new NameAlreadyBoundException(" id Attribute values must be unique but value " + value
							+ " already exists for entity " + this.entity.getFullName());
				} else {
					//Add this Id Attribute vale to namespace
					this.entity.getInstanceNamespace().add(value);
				}
			}
			//Add value to instance
			this.values.put(attr, value);
		} else {
			throw new AttributeNonExistentException(this.entity.getFullName() + " does not have attribute "
					+ attr.getName() + "(" + attr.getLabel() + ")");
		}
	}
	
	public void removeValue(Attribute attr){
			this.values.remove(attr);
	}

	public String getValue(Attribute attr){
		
		return this.values.get(attr);
		
	}
	
	public HashMap<Attribute,String> removeInvalidAttributes(){
		HashMap<Attribute,String> invalidValues = new HashMap<Attribute,String>();
		for(Attribute attr:this.values.keySet()){
			if(!this.entity.getAttributes().contains(attr)){
				invalidValues.put(attr, this.values.get(attr));
				this.values.keySet().remove(attr);
			}
		}
		return invalidValues;
	}
	
	public void fillAllAttributes(){
		for(Attribute attr:this.entity.getAttributes()){
			if(!this.values.containsKey(attr)){
				this.values.put(attr, "");
			}
		}
	}
	
}
