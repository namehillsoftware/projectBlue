package com.lasthopesoftware.bluewater;

import org.slf4j.LoggerFactory;
import com.lasthopesoftware.bluewater.exceptions.LoggerUncaughtExceptionHandler;
import android.annotation.SuppressLint;
import android.app.Application;
import android.os.StrictMode;

public class MainApplication extends Application {
	
	private static final boolean DEVELOPER_MODE = true;
	
	@SuppressLint("DefaultLocale")
	@Override
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(new LoggerUncaughtExceptionHandler());
		LoggerFactory.getLogger(MainApplication.class).info("Uncaught exceptions logging to custom uncaught exception handler.");
		
		if (DEVELOPER_MODE) {
	         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                 .detectDiskReads()
	                 .detectDiskWrites()
	                 .detectNetwork()   // or .detectAll() for all detectable problems
	                 .penaltyLog()
	                 .build());
	         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                 .detectLeakedSqlLiteObjects()
	                 .detectLeakedClosableObjects()
	                 .penaltyLog()
//	                 .penaltyDeath()
	                 .build());
	     }
	}
}
