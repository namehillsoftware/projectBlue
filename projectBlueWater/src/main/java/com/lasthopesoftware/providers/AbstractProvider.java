package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.vedsoft.fluent.FluentTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by david on 11/26/15.
 */
public abstract class AbstractProvider<T> extends FluentTask<String, Void, T> {

	private final IConnectionProvider connectionProvider;
	private static final ExecutorService providerExecutor = Executors.newSingleThreadExecutor();

	protected AbstractProvider(IConnectionProvider connectionProvider, String... params) {
		super(providerExecutor, params);

		this.connectionProvider = connectionProvider;
	}

	@Override
	protected final T executeInBackground(String[] params) {
		return getData(connectionProvider, params);
	}

	protected abstract T getData(IConnectionProvider connectionProvider, String[] params);
}