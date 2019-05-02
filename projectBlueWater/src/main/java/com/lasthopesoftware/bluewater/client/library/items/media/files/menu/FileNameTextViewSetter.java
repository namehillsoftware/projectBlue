package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

import android.os.Handler;
import android.widget.TextView;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CancellationException;

public class FileNameTextViewSetter {

	private static final Logger logger = LoggerFactory.getLogger(FileNameTextViewSetter.class);

	private final TextView textView;
	private final Handler handler;

	private volatile Promise<Void> currentlyPromisedTextViewUpdate;

	public FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
	}

	public synchronized Promise<Void> promiseTextViewUpdate(ServiceFile serviceFile) {
		if (currentlyPromisedTextViewUpdate != null)
			currentlyPromisedTextViewUpdate.cancel();

		return currentlyPromisedTextViewUpdate = new PromisedTextViewUpdate(textView, handler, serviceFile);
	}

	private class PromisedTextViewUpdate extends Promise<Void> implements Runnable {

		private final CancellationProxy cancellationProxy = new CancellationProxy();
		private final TextView textView;
		private final Handler handler;
		private final ServiceFile serviceFile;

		PromisedTextViewUpdate(TextView textView, Handler handler, ServiceFile serviceFile) {
			this.textView = textView;
			this.handler = handler;
			this.serviceFile = serviceFile;

			respondToCancellation(cancellationProxy);

			this.handler.post(this);
		}

		@Override
		public void run() {
			textView.setText(R.string.lbl_loading);

			SessionConnection.getInstance(textView.getContext()).promiseSessionConnection()
				.eventually(connectionProvider -> {
					if (isUpdateCancelled()) {
						resolve(null);
						return Promise.empty();
					}

					final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
					final CachedFilePropertiesProvider cachedFilePropertiesProvider =
						new CachedFilePropertiesProvider(connectionProvider, filePropertyCache,
							new FilePropertiesProvider(connectionProvider, filePropertyCache, ParsingScheduler.instance()));

					final Promise<Map<String, String>> filePropertiesPromise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile);

					cancellationProxy.doCancel(filePropertiesPromise);

					return filePropertiesPromise;
				})
				.eventually(LoopedInPromise.response(properties -> {
					if (isUpdateCancelled()) {
						resolve(null);
						return null;
					}

					final String fileName = properties.get(FilePropertiesProvider.NAME);

					if (fileName != null)
						textView.setText(fileName);

					resolve(null);
					return null;
				}, handler))
				.excuse(e -> {
					if (!(e instanceof CancellationException))
						logger.error("An error occurred getting the file properties for the file with ID " + serviceFile.getKey(), e);

					resolve(null);

					return null;
				});
		}

		private boolean isUpdateCancelled() {
			return currentlyPromisedTextViewUpdate != this
				|| cancellationProxy.isCancelled();
		}
	}
}
