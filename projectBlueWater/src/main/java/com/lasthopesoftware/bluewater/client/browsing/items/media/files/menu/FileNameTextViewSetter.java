package com.lasthopesoftware.bluewater.client.browsing.items.media.files.menu;

import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.exceptions.LoggerUncaughtExceptionHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CancellationException;

import javax.net.ssl.SSLProtocolException;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;
import static com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.delay;

public class FileNameTextViewSetter {

	private static final Logger logger = LoggerFactory.getLogger(FileNameTextViewSetter.class);

	private static final Duration timeoutDuration = Duration.standardMinutes(1);

	private final TextView textView;
	private final Handler handler;

	private volatile Promise<Void> currentlyPromisedTextViewUpdate = Promise.empty();

	public FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
	}

	public synchronized Promise<Void> promiseTextViewUpdate(ServiceFile serviceFile) {
		currentlyPromisedTextViewUpdate.cancel();

		currentlyPromisedTextViewUpdate = currentlyPromisedTextViewUpdate
			.eventually(v -> new PromisedTextViewUpdate(serviceFile));

		return currentlyPromisedTextViewUpdate;
	}

	private class PromisedTextViewUpdate extends Promise<Void> implements Runnable {

		private final CancellationProxy cancellationProxy = new CancellationProxy();
		private final ServiceFile serviceFile;

		PromisedTextViewUpdate(ServiceFile serviceFile) {
			this.serviceFile = serviceFile;

			respondToCancellation(cancellationProxy);

			if (handler.getLooper().getThread() == Thread.currentThread())
				run();
			else
				handler.post(this);
		}

		@Override
		public void run() {
			textView.setText(R.string.lbl_loading);

			final Promise<Void> promisedViewSet =
				SessionConnection.getInstance(textView.getContext()).promiseSessionConnection()
					.eventually(connectionProvider -> {
						if (cancellationProxy.isCancelled()) {
							resolve(null);
							return new Promise<>(Collections.emptyMap());
						}

						final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
						final CachedSessionFilePropertiesProvider cachedSessionFilePropertiesProvider =
							new CachedSessionFilePropertiesProvider(connectionProvider, filePropertyCache,
								new SessionFilePropertiesProvider(connectionProvider, filePropertyCache, ParsingScheduler.instance()));

						final Promise<Map<String, String>> filePropertiesPromise = cachedSessionFilePropertiesProvider.promiseFileProperties(serviceFile);

						cancellationProxy.doCancel(filePropertiesPromise);

						return filePropertiesPromise;
					})
					.eventually(LoopedInPromise.response(properties -> {
						if (cancellationProxy.isCancelled()) return null;

						final String fileName = properties.get(KnownFileProperties.NAME);

						if (fileName != null)
							textView.setText(fileName);

						return null;
					}, handler));

			final Promise<Void> delayPromise = delay(timeoutDuration);
			cancellationProxy.doCancel(delayPromise);

			Promise.whenAny(promisedViewSet, delayPromise)
				.must(() -> {
					cancellationProxy.run();
					resolve(null);
				})
				.excuse(forward())
				.eventually(e -> new QueuedPromise<>(() -> {
					if (cancellationProxy.isCancelled()) return null;

					if (e instanceof CancellationException) return null;

					if (e instanceof SocketException) {
						final String message = e.getMessage();
						if (message != null && message.toLowerCase().contains("socket closed"))
							return null;
					}

					if (e instanceof IOException) {
						final String message = e.getMessage();
						if (message != null && message.toLowerCase().contains("canceled"))
							return null;
					}

					if (e instanceof SSLProtocolException) {
						final String message = e.getMessage();
						if (message != null && message.toLowerCase().contains("ssl handshake aborted"))
							return null;
					}

					logger.error("An error occurred getting the file properties for the file with ID " + serviceFile.getKey(), e);

					return null;
				}, LoggerUncaughtExceptionHandler.getErrorExecutor()));
		}
	}
}
