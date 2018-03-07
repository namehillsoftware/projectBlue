package com.lasthopesoftware.resources.loopers;

import android.os.Looper;

import com.namehillsoftware.handoff.promises.Promise;

public class LooperThreadCreator {

	public static Promise<Looper> promiseNewLooperThread(String looperThreadName) {
		return new Promise<>(m -> new Thread(() -> {
			try {
				Looper.prepare();
				m.sendResolution(Looper.myLooper());
				Looper.loop();
			} catch (Throwable t) {
				m.sendRejection(t);
			}
		}, looperThreadName).start());
	}
}
