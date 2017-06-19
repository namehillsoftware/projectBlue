package com.lasthopesoftware.providers;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.QueuedPromise;
import com.lasthopesoftware.promises.Messenger;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractProvider<Data> {

	public static final ExecutorService providerExecutor = Executors.newSingleThreadExecutor();

	private final ProvideDataTask<Data> provideDataTask;

	AbstractProvider(IConnectionProvider connectionProvider, String... params) {
		provideDataTask = new ProvideDataTask<>(this, connectionProvider, params);
	}

	public final Promise<Data> promiseData() {
		return new QueuedPromise<>(provideDataTask, providerExecutor);
	}

	protected abstract Data getData(IConnectionProvider connectionProvider, Cancellation cancellation, String[] params) throws Throwable;

	private static class ProvideDataTask<Data> implements
		OneParameterAction<Messenger<Data>>,
		Runnable {
		private final AbstractProvider<Data> provider;
		private final IConnectionProvider connectionProvider;
		private final String[] params;
		private final Cancellation cancellation = new Cancellation();

		ProvideDataTask(AbstractProvider<Data> provider, IConnectionProvider connectionProvider, String... params) {
			this.provider = provider;
			this.connectionProvider = connectionProvider;
			this.params = params;
		}

		@Override
		public void runWith(Messenger<Data> messenger) {
			messenger.cancellationRequested(this);

			try {
				messenger.sendResolution(provider.getData(connectionProvider, cancellation, params));
			} catch (Throwable e) {
				messenger.sendRejection(e);
			}
		}

		@Override
		public void run() {
			cancellation.cancel();
		}
	}
}