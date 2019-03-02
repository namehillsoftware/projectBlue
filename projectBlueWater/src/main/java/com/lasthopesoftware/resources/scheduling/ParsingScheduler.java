package com.lasthopesoftware.resources.scheduling;

import android.os.Build;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.util.concurrent.*;

public class ParsingScheduler implements ScheduleParsingWork {

	private static final CreateAndHold<Executor> executor = new Lazy<>(() -> {
		final int maxThreadPoolSize = Runtime.getRuntime().availableProcessors();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return new ForkJoinPool
				(maxThreadPoolSize,
					ForkJoinPool.defaultForkJoinWorkerThreadFactory,
					null, true);
		}

		return new ThreadPoolExecutor(
			0, maxThreadPoolSize,
			1, TimeUnit.MINUTES,
			new LinkedBlockingQueue<>());
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
