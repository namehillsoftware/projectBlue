package com.lasthopesoftware.bluewater.shared.promises;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Duration;

import java.util.Timer;
import java.util.TimerTask;

public final class PromiseDelay<Response> extends Promise<Response> {
	private static final CreateAndHold<Timer> delayTimer = new Lazy<>(Timer::new);

	public static <Response> Promise<Response> delay(Duration delay) {
		return new PromiseDelay<>(delay);
	}

	private PromiseDelay(Duration delay) {
		final DelayedResolution delayedResolution = new DelayedResolution();
		delayTimer.getObject().schedule(delayedResolution, delay.getMillis());
		respondToCancellation(delayedResolution::cancel);
	}

	private final class DelayedResolution extends TimerTask {

		@Override
		public void run() {
			resolve(null);
		}

		@Override
		public boolean cancel() {
			return super.cancel();
		}
	}
}
