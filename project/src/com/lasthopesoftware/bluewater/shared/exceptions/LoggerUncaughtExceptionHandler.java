package com.lasthopesoftware.bluewater.shared.exceptions;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.LoggerFactory;

public class LoggerUncaughtExceptionHandler implements UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		LoggerFactory.getLogger(LoggerUncaughtExceptionHandler.class).error("Uncaught Exception", ex);
	}

}
