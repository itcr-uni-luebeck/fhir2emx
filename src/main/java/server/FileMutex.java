/*******************************************************************************
 * Copyright (c) 2017 - IT Center for Clinical Research, University of Luebeck
 * Noemi Deppenwiese, Hannes Ulrich
 ******************************************************************************/
package server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FileMutex {
	
	private final static ReentrantLock fileLock = new ReentrantLock(true);

	public static void lock(){
		fileLock.lock();
	}
	
	public static void unlock(){
		fileLock.unlock();
	}
	
	public static void tryLock(int milliSeconds) throws InterruptedException{
		fileLock.tryLock(milliSeconds, TimeUnit.MILLISECONDS);
	}

}
