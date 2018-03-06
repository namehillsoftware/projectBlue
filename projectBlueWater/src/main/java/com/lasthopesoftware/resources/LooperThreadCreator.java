package com.lasthopesoftware.resources;

import android.os.Looper;

import com.namehillsoftware.handoff.promises.Promise;

public class LooperThreadCreator {

	public static Promise<Looper> promiseNewLooperThread(String looperThreadName) {
		return new Promise<>(m -> new Thread(() -> {
			Looper.prepare();
			m.sendResolution(Looper.myLooper());
		}, looperThreadName));
	}
}
