package com.lasthopesoftware.bluewater.shared.exceptions;

import com.namehillsoftware.handoff.rejections.UnhandledRejectionsReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

public class LoggerUncaughtExceptionHandler implements UncaughtExceptionHandler, UnhandledRejectionsReceiver {

	private static final Logger logger = LoggerFactory.getLogger(LoggerUncaughtExceptionHandler.class);

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		logger.error("Uncaught Exception", ex);
	}

	@Override
	public void newUnhandledRejection(Throwable rejection) {
		logger.warn("An asynchronous exception has not yet been handled", rejection);
	}
}
