package com.lasthopesoftware.resources.loopers;

import android.os.HandlerThread;

import com.namehillsoftware.handoff.promises.Promise;

public class HandlerThreadCreator {

	public static Promise<HandlerThread> promiseNewHandlerThread(String looperThreadName) {
		return new Promise<>(m -> new HandlerThread(looperThreadName) {
			@Override
			protected void onLooperPrepared() {
				try {
					m.sendResolution(this);
				} catch (Throwable t) {
					m.sendRejection(t);
				}
			}
		}.start());
	}
}
