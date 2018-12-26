package com.lasthopesoftware.resources.scheduling;

import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ParsingScheduler implements ScheduleParsingWork {

	private static final CreateAndHold<Executor> executor = new Lazy<>(Executors::newCachedThreadPool);
	private static final CreateAndHold<ParsingScheduler> scheduler = new Lazy<>(ParsingScheduler::new);

	public static ParsingScheduler instance() {
		return scheduler.getObject();
	}

	private ParsingScheduler() {}

	@Override
	public Executor getScheduler() {
		return executor.getObject();
	}
}
