package com.lasthopesoftware.bluewater.shared.exceptions;

import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

public class LoggerUncaughtExceptionHandler implements UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		LoggerFactory.getLogger(LoggerUncaughtExceptionHandler.class).error("Uncaught Exception", ex);
	}

}
