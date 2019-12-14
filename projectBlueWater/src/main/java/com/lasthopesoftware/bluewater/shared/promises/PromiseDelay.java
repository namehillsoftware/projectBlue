package com.lasthopesoftware.bluewater.shared.promises;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Duration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class PromiseDelay<Response> extends Promise<Response> {
	private static final CreateAndHold<ScheduledExecutorService> delayScheduler =
		new Lazy<>(() -> Executors.newScheduledThreadPool(0));

	public static <Response> Promise<Response> delay(Duration delay) {
		return new PromiseDelay<>(delay);
	}

	private PromiseDelay(Duration delay) {
		final ScheduledFuture<?> future = delayScheduler.getObject()
			.schedule(
				() -> resolve(null),
				delay.getMillis(),
				TimeUnit.MILLISECONDS);

		respondToCancellation(() -> future.cancel(false));
	}
}
