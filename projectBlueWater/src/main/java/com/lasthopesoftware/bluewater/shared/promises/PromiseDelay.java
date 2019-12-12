package com.lasthopesoftware.bluewater.shared.promises;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Duration;

import java.util.Timer;
import java.util.TimerTask;

public class PromiseDelay<Response> extends Promise<Response> {
	private static final CreateAndHold<Timer> delayTimer = new Lazy<>(Timer::new);

	public static <Response> Promise<Response> delay(Duration delay) {
		return new PromiseDelay<>(delay);
	}

	private PromiseDelay(Duration delay) {
		delayTimer.getObject().schedule(new DelayedResolution(), delay.getMillis());
	}

	private class DelayedResolution extends TimerTask {

		@Override
		public void run() {
			resolve(null);
		}
	}
}
