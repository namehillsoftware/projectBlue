package com.lasthopesoftware.resources;

import android.content.Context;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.namehillsoftware.lazyj.Lazy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ScopedLocalBroadcastManagerContainer {
	private static final Lazy<Constructor<LocalBroadcastManager>> lazyConstructor = new Lazy<>(() -> {
		Constructor<LocalBroadcastManager> broadcastManagerConstructor = LocalBroadcastManager.class.getDeclaredConstructor(Context.class);
		broadcastManagerConstructor.setAccessible(true);
		return broadcastManagerConstructor;
	});

	public static ScopedLocalBroadcastManagerContainer newScopedBroadcastManager(Context context) throws IllegalAccessException, InvocationTargetException, InstantiationException {
		return new ScopedLocalBroadcastManagerContainer(context);
	}

	private final Lazy<LocalBroadcastManager> localBroadcastManager;

	private final Lazy<Handler> handler;

	public ScopedLocalBroadcastManagerContainer(Context context) {

		localBroadcastManager = new Lazy<>(() -> lazyConstructor.getObject().newInstance(context));
		handler = new Lazy<>(() -> new Handler(context.getMainLooper()));
	}

	public LocalBroadcastManager getLocalBroadcastManager() {
		return localBroadcastManager.getObject();
	}

	public void processMessages() {
		if (localBroadcastManager.isCreated())
			handler.getObject().sendEmptyMessage(1);
	}
}
