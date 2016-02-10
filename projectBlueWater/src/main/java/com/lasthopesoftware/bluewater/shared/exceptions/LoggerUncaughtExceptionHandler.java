package com.lasthopesoftware.bluewater.shared.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

public class LoggerUncaughtExceptionHandler implements UncaughtExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(LoggerUncaughtExceptionHandler.class);

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		logger.error("Uncaught Exception", ex);
	}

}
