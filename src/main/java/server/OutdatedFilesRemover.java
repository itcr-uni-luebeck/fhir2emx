/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package server;

import java.io.File;

import org.apache.commons.io.filefilter.AgeFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutdatedFilesRemover implements Runnable {
	
	final static Logger logger = LoggerFactory.getLogger(OutdatedFilesRemover.class);
	private File directory;
	private int olderThanMinutes;

	public OutdatedFilesRemover(int olderThanMinutes) {
		//Remove last char ("/") to get vaild directory location
		String dir = FileHandler.getEMXFilesLocation().replaceFirst(".$","");
		this.directory = new File(dir);
		this.olderThanMinutes = olderThanMinutes;
		logger.debug("Initializing File Remover for "+dir);
	}

	@Override
	public void run() {
		logger.debug("Running EMX file cleanup...");
		if(this.directory.exists() && this.directory.isDirectory()){
			//HOURS - Minutes - Seconds - Millis
			long cutoff = System.currentTimeMillis() - (olderThanMinutes * 60 * 1000);
			 String[] files = directory.list( new AgeFileFilter(cutoff) );
			 
			 for ( int i = 0; i < files.length; i++ ) {
			     File file = new File(directory.getAbsolutePath()+File.separator+files[i]);
			     logger.trace("Deleting file "+files[i]);
			     try {
					FileMutex.tryLock(500);
					if( file.delete()){
				    	logger.debug("Deleted File"+files[i]);
				    }else{
				    	logger.warn("Failed to delete File "+files[i]);
				    }
				    FileMutex.unlock();
				} catch (InterruptedException e) {
					logger.warn("Unable to get File Mutex. Skipping file "+files[i]+" for now.");
				}
			    
			 }
			 return;
		}else{
			logger.warn("Directory "+this.directory.getAbsolutePath()+" does not exist or is not a directory. Aborting cleanup.");
			return;
		}
	}

}
