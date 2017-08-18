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
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileNameTextViewSetter {

	private static final Logger logger = LoggerFactory.getLogger(FileNameTextViewSetter.class);

	private final TextView textView;
	private final Handler handler;

	private Promise<Map<String, String>> currentlyPromisedTextViewUpdate;

	public FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
	}

	public Promise<Map<String, String>> promiseTextViewUpdate(ServiceFile serviceFile) {
		textView.setText(R.string.lbl_loading);

		if (currentlyPromisedTextViewUpdate == null) {
			currentlyPromisedTextViewUpdate = new Promise<>(new LockedTextViewTask(textView, handler, serviceFile));
			return currentlyPromisedTextViewUpdate;
		}

		AsyncTask.THREAD_POOL_EXECUTOR.execute(currentlyPromisedTextViewUpdate::cancel);

		final Promise<Map<String, String>> successContinuation =
			currentlyPromisedTextViewUpdate.eventually(o -> new Promise<>(new LockedTextViewTask(textView, handler, serviceFile)));

		final Promise<Map<String, String>> failureContinuation =
			currentlyPromisedTextViewUpdate
				.excuse(r -> {
					if (r instanceof CancellationException) return null;

					logger.warn("Last promised text view update was cancelled, but an error occurred", r);
					return null;
				})
				.eventually(o ->  new Promise<>(new LockedTextViewTask(textView, handler, serviceFile)));

		currentlyPromisedTextViewUpdate = Promise.whenAny(successContinuation, failureContinuation);
		return currentlyPromisedTextViewUpdate;
	}

	private static class LockedTextViewTask implements OneParameterAction<Messenger<Map<String, String>>> {

		private final TextView textView;
		private final Handler handler;
		private final ServiceFile serviceFile;

		LockedTextViewTask(TextView textView, Handler handler, ServiceFile serviceFile) {
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
				return;
			}

			final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
			final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

			if (cancellationProxy.isCancelled()) {
				messenger.sendRejection(new CancellationException("`FileNameTextViewSetter` was cancelled"));
				return;
			}

			final Promise<Map<String, String>> filePropertiesPromise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());

			cancellationProxy.doCancel(filePropertiesPromise);

			filePropertiesPromise.eventually(properties -> new LoopedInPromise<>(() -> {
				final String fileName = properties.get(FilePropertiesProvider.NAME);

				if (fileName != null && !cancellationProxy.isCancelled())
					textView.setText(fileName);

				messenger.sendResolution(properties);
				return null;
			}, handler))
			.excuse(e -> {
				messenger.sendRejection(e);
				return null;
			});
		}
	}
}
