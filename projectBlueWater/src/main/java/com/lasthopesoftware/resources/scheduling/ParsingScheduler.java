package com.lasthopesoftware.resources.scheduling;

import android.os.Build;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class ParsingScheduler implements ScheduleParsingWork {

	private static final CreateAndHold<Executor> executor = new Lazy<>(() -> {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return new ForkJoinPool
				(Runtime.getRuntime().availableProcessors(),
					ForkJoinPool.defaultForkJoinWorkerThreadFactory,
					null, true);
		}

		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	});

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
