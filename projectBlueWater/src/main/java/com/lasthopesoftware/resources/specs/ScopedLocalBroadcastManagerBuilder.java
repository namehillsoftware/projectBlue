package com.lasthopesoftware.resources.specs;

import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.namehillsoftware.lazyj.Lazy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ScopedLocalBroadcastManagerBuilder {
	private static final Lazy<Constructor<LocalBroadcastManager>> lazyConstructor = new Lazy<>(() -> {
		Constructor<LocalBroadcastManager> broadcastManagerConstructor = LocalBroadcastManager.class.getDeclaredConstructor(Context.class);
		broadcastManagerConstructor.setAccessible(true);
		return broadcastManagerConstructor;
	});

	public static LocalBroadcastManager newScopedBroadcastManager(Context context) throws IllegalAccessException, InvocationTargetException, InstantiationException {
		return lazyConstructor.getObject().newInstance(context);
	}
}
