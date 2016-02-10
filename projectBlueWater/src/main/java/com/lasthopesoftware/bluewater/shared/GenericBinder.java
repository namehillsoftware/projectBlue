package com.lasthopesoftware.bluewater.shared;

import android.app.Service;
import android.os.Binder;

/**
 * Created by david on 8/19/15.
 */
public class GenericBinder<TService extends Service> extends Binder {
	private final TService service;

	public GenericBinder(TService service) {
		super();

		this.service = service;
	}

	TService getService() {
		return service;
	}
}
