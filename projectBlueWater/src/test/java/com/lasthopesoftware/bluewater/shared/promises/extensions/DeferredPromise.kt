package com.lasthopesoftware.bluewater.shared.promises.extensions;

import com.namehillsoftware.handoff.promises.Promise;

public class DeferredPromise<Resolution> extends Promise<Resolution> {
	private final Resolution resolution;
	private final Throwable error;

	public DeferredPromise(Resolution resolution) {
		this.resolution = resolution;
		this.error = null;
	}

	public DeferredPromise(Throwable error) {
		this.resolution = null;
		this.error = error;
	}

	public void resolve() {
		if (this.resolution != null) resolve(resolution);
		else reject(error);
	}
}
