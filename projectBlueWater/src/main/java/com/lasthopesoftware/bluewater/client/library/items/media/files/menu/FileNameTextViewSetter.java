package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

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
import com.lasthopesoftware.messenger.promises.MessengerTask;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.propagation.CancellationProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CancellationException;

public class FileNameTextViewSetter {

	private static final Logger logger = LoggerFactory.getLogger(FileNameTextViewSetter.class);

	private final TextView textView;
	private final Handler handler;

	private Promise<Void> currentlyPromisedTextViewUpdate;

	public FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
	}

	public Promise<Void> promiseTextViewUpdate(ServiceFile serviceFile) {
		if (currentlyPromisedTextViewUpdate == null) {
			currentlyPromisedTextViewUpdate = new LoopedInPromise<>(new LockedTextViewTask(textView, handler, serviceFile), handler);
			return currentlyPromisedTextViewUpdate;
		}

		currentlyPromisedTextViewUpdate.cancel();

		currentlyPromisedTextViewUpdate =
			currentlyPromisedTextViewUpdate.eventually(o -> new LoopedInPromise<>(new LockedTextViewTask(textView, handler, serviceFile), handler));

		return currentlyPromisedTextViewUpdate;
	}

	private static class LockedTextViewTask implements MessengerTask<Void> {

		private final TextView textView;
		private final Handler handler;
		private final ServiceFile serviceFile;

		LockedTextViewTask(TextView textView, Handler handler, ServiceFile serviceFile) {
			this.textView = textView;
			this.handler = handler;
			this.serviceFile = serviceFile;
		}

		@Override
		public void execute(Messenger<Void> messenger) {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			messenger.cancellationRequested(cancellationProxy);

			textView.setText(R.string.lbl_loading);

			final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
			final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

			if (cancellationProxy.isCancelled()) {
				messenger.sendResolution(null);
				return;
			}

			final Promise<Map<String, String>> filePropertiesPromise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());

			cancellationProxy.doCancel(filePropertiesPromise);

			filePropertiesPromise.eventually(LoopedInPromise.response(properties -> {
				final String fileName = properties.get(FilePropertiesProvider.NAME);

				if (fileName != null)
					textView.setText(fileName);

				messenger.sendResolution(null);
				return null;
			}, handler))
			.excuse(e -> {
				if (!(e instanceof CancellationException))
					logger.error("An error occurred getting the file properties for the file with ID " + serviceFile.getKey(), e);

				messenger.sendResolution(null);

				return null;
			});
		}
	}
}
