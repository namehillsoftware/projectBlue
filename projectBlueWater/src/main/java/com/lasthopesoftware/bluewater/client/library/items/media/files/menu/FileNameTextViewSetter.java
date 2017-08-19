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
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.propagation.CancellationProxy;
import com.vedsoft.futures.runnables.OneParameterAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
			currentlyPromisedTextViewUpdate = new Promise<>(new LockedTextViewTask(textView, handler, serviceFile));
			return currentlyPromisedTextViewUpdate;
		}

		currentlyPromisedTextViewUpdate.cancel();

		currentlyPromisedTextViewUpdate =
			currentlyPromisedTextViewUpdate.eventually(o -> new Promise<>(new LockedTextViewTask(textView, handler, serviceFile)));

		return currentlyPromisedTextViewUpdate;
	}

	private static class LockedTextViewTask implements OneParameterAction<Messenger<Void>> {

		private final TextView textView;
		private final Handler handler;
		private final ServiceFile serviceFile;

		LockedTextViewTask(TextView textView, Handler handler, ServiceFile serviceFile) {
			this.textView = textView;
			this.handler = handler;
			this.serviceFile = serviceFile;
		}

		@Override
		public void runWith(Messenger<Void> messenger) {
			final CancellationProxy cancellationProxy = new CancellationProxy();
			messenger.cancellationRequested(cancellationProxy);

			if (handler.getLooper().getThread() == Thread.currentThread())
				textView.setText(R.string.lbl_loading);
			else
				handler.postAtFrontOfQueue(() -> textView.setText(R.string.lbl_loading));

			final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
			final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

			if (cancellationProxy.isCancelled()) {
				messenger.sendResolution(null);
				return;
			}

			final Promise<Map<String, String>> filePropertiesPromise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());

			cancellationProxy.doCancel(filePropertiesPromise);

			filePropertiesPromise.eventually(properties -> new LoopedInPromise<>(() -> {
				final String fileName = properties.get(FilePropertiesProvider.NAME);

				if (fileName != null)
					textView.setText(fileName);

				messenger.sendResolution(null);
				return null;
			}, handler))
			.excuse(e -> {
				messenger.sendResolution(null);

				return null;
			});
		}
	}
}
