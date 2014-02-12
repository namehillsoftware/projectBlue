package com.lasthopesoftware.bluewater;

import org.slf4j.LoggerFactory;

import com.lasthopesoftware.bluewater.exceptions.LoggerUncaughtExceptionHandler;

import android.app.Application;

public class MainApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Thread.setDefaultUncaughtExceptionHandler(new LoggerUncaughtExceptionHandler());
		LoggerFactory.getLogger(MainApplication.class).info("Uncaught exceptions logging to custom uncaught exception handler.");
	}
}
