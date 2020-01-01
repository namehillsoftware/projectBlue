package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedSessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.SessionFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLProtocolException;

import static com.lasthopesoftware.bluewater.shared.promises.ForwardedResponse.forward;
import static com.lasthopesoftware.bluewater.shared.promises.PromiseDelay.delay;

public class FileNameTextViewSetter {

	private static final Logger logger = LoggerFactory.getLogger(FileNameTextViewSetter.class);
	private static final CreateAndHold<Executor> errorExecutor = new Lazy<>(CachedSingleThreadExecutor::new);

	private static final Duration timeoutDuration = Duration.standardMinutes(1);

	private final TextView textView;
	private final Handler handler;

	private volatile PromisedTextViewUpdate currentlyPromisedTextViewUpdate;

	public FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
	}

	public Promise<Void> promiseTextViewUpdate(ServiceFile serviceFile) {
		if (currentlyPromisedTextViewUpdate != null)
			currentlyPromisedTextViewUpdate.cancel();

		currentlyPromisedTextViewUpdate = new PromisedTextViewUpdate(serviceFile);
		currentlyPromisedTextViewUpdate.beginUpdate();
		return currentlyPromisedTextViewUpdate;
	}

	private class PromisedTextViewUpdate extends Promise<Void> implements Runnable {

		private final CancellationProxy cancellationProxy = new CancellationProxy();
		private final ServiceFile serviceFile;

		PromisedTextViewUpdate(ServiceFile serviceFile) {
			this.serviceFile = serviceFile;

			respondToCancellation(cancellationProxy);
		}

		void beginUpdate() {
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
						if (isUpdateCancelled()) {
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
						if (isUpdateCancelled()) return resolve();

						final String fileName = properties.get(SessionFilePropertiesProvider.NAME);

						if (fileName != null)
							textView.setText(fileName);

						return resolve();
					}, handler));

			final Promise<Void> delayPromise = delay(timeoutDuration);
			cancellationProxy.doCancel(delayPromise);

			Promise.whenAny(promisedViewSet, delayPromise)
				.then(v -> {
					cancellationProxy.run();
					return resolve();
				})
				.excuse(forward())
				.eventually(e -> new QueuedPromise<>(() -> {
					if (isUpdateCancelled()) return resolve();

					if (e instanceof CancellationException) return resolve();

					if (e instanceof SocketException) {
						final String message = e.getMessage();
						if (message != null && message.toLowerCase().contains("socket closed"))
							return resolve();
					}

					if (e instanceof IOException) {
						final String message = e.getMessage();
						if (message != null && message.toLowerCase().contains("canceled"))
							return resolve();
					}

					if (e instanceof SSLProtocolException) {
						final String message = e.getMessage();
						if (message != null && message.toLowerCase().contains("ssl handshake aborted"))
							return resolve();
					}

					logger.error("An error occurred getting the file properties for the file with ID " + serviceFile.getKey(), e);

					return resolve();
				}, errorExecutor.getObject()));
		}

		private Void resolve() {
			resolve(null);
			return null;
		}

		private boolean isUpdateCancelled() {
			return currentlyPromisedTextViewUpdate != this
				|| cancellationProxy.isCancelled();
		}
	}
}
