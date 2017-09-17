/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHandler {
	
	final static Logger logger = LoggerFactory.getLogger(FileHandler.class);
	
	static ByteArrayOutputStream getExistingFileAsStream(String id){
		File file = new File(getEMXFilesLocation() + id + ".zip");
		logger.debug("looking for file at " + getEMXFilesLocation() + id + ".zip");
		if (!file.exists()) {
			return null;
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			FileMutex.tryLock(500);
		} catch (InterruptedException e1) {
			logger.warn("Unable to lock File Mutex.");
			return null;
		}
		try {
			FileInputStream fis = new FileInputStream(file);
			IOUtils.copy(fis, baos);
			fis.close();
		} catch (IOException e) {
			logger.warn("Error reading file");
			return null;
		} finally {
			FileMutex.unlock();
		}
		return baos;
	}
	
	
	private static void writeStreamToFile(ByteArrayOutputStream baos, String filename) throws IOException {
		//Get static file Location
		String dirPath = getEMXFilesLocation();
		String filePath = dirPath+filename;
		logger.debug("Writing file to "+ filePath);

		//Write data
		OutputStream outputStream = new FileOutputStream(filePath);
		baos.writeTo(outputStream);
		
	}
	

	static String saveAsFile(Map<String, String> emxTables){
		
		String is = (new Integer(emxTables.hashCode())).toString();
		try {
			writeStreamToFile(prepareEMXFile(emxTables),is+".zip");
		} catch (IOException e) {
			logger.error("Error writing generated EMX file",e);
		}
		return is;
	}
	
	public static String getEMXFilesLocation(){
		//Get static file Location
		URL dokuURL = Main.class.getResource("/public/fhir2emx/apiDoku.html");
		int split = dokuURL.getPath().lastIndexOf("/");
		String dirPath = dokuURL.getPath().substring(0,split);
		String emxPath =  dirPath+"/tables";
		File dir = new File(emxPath);
		if(!dir.exists()){
			dir.mkdir();
		}
		
		return emxPath+"/";
	}
	
	static ByteArrayOutputStream prepareEMXFile(Map<String, String> emxTables){
		
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {

			//Iterate over all table names and add them to zip Stream
			for (String s : emxTables.keySet()) {
				ZipEntry entry = new ZipEntry(s+".tsv");
				zos.putNextEntry(entry);
				zos.write(emxTables.get(s).getBytes());
				zos.closeEntry();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return baos;
		
		
	}

}
