package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

import android.os.AsyncTask;
import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.propagation.CancellationProxy;
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileNameTextViewSetter {

	private final Lock lock = new ReentrantLock();

	private final TextView textView;
	private final Handler handler;

	public FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
	}

	public Promise<Map<String, String>> promiseTextViewUpdate(ServiceFile serviceFile) {
		lock.lock();

		textView.setText(R.string.lbl_loading);
		return new QueuedPromise<>(new LockedTextViewTask(lock, textView, handler, serviceFile), AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static class LockedTextViewTask implements OneParameterAction<Messenger<Map<String, String>>> {

		private final Lock activeLock;
		private final TextView textView;
		private final Handler handler;
		private final ServiceFile serviceFile;

		LockedTextViewTask(Lock activeLock, TextView textView, Handler handler, ServiceFile serviceFile) {
			this.activeLock = activeLock;
			this.textView = textView;
			this.handler = handler;
			this.serviceFile = serviceFile;
		}

		@Override
		public void runWith(Messenger<Map<String, String>> messenger) {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			messenger.cancellationRequested(cancellationProxy);

			if (cancellationProxy.isCancelled()) {
				messenger.sendRejection(new CancellationException("`FileNameTextViewSetter` was cancelled"));
				activeLock.unlock();
				return;
			}

			final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
			final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

			if (cancellationProxy.isCancelled()) {
				messenger.sendRejection(new CancellationException("`FileNameTextViewSetter` was cancelled"));
				activeLock.unlock();
				return;
			}

			final Promise<Map<String, String>> filePropertiesPromise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());

			cancellationProxy.doCancel(filePropertiesPromise);

			final Promise<Map<String, String>> textViewUpdatePromise =
				filePropertiesPromise.eventually(properties -> new LoopedInPromise<>(ct -> {
					final String fileName = properties.get(FilePropertiesProvider.NAME);

					if (fileName != null && !ct.isCancelled())
						textView.setText(fileName);

					return properties;
				}, handler));

			cancellationProxy.doCancel(textViewUpdatePromise);

			textViewUpdatePromise
				.then(props -> {
					if (cancellationProxy.isCancelled())
						textView.setText(R.string.lbl_loading);

					messenger.sendResolution(props);
					activeLock.unlock();
					return null;
				})
				.excuse(e -> {
					messenger.sendRejection(e);
					activeLock.unlock();
					return null;
				});
		}
	}
}
