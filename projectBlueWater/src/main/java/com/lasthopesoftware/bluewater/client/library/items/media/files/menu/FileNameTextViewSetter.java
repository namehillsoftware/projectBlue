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
import com.lasthopesoftware.bluewater.shared.promises.extensions.DispatchedPromise;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.propagation.CancellationProxy;

import java.util.Map;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class FileNameTextViewSetter implements Runnable {

	private final TextView textView;

	public static Promise<Map<String, String>> startNew(ServiceFile serviceFile, TextView textView) {
		final FileNameTextViewSetter fileNameTextViewSetter = new FileNameTextViewSetter(textView);
		final Handler handler = new Handler(textView.getContext().getMainLooper());

		fileNameTextViewSetter.setLoading();

		return new Promise<>((messenger) -> {
			final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
			final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
			final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

			final Promise<Map<String, String>> promise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());
			promise.next(runCarelessly(messenger::sendResolution));
			promise.error(runCarelessly(messenger::sendRejection));

			final CancellationProxy cancellationProxy = new CancellationProxy();
			cancellationProxy.doCancel(promise);

			final Promise<Void> textViewUpdatePromise =
				promise.then(properties -> new DispatchedPromise<>((cancellableToken) -> {
					final String fileName = properties.get(FilePropertiesProvider.NAME);

					if (!cancellableToken.isCancelled() && fileName != null)
						textView.setText(fileName);

					return null;
				}, handler));

			cancellationProxy.doCancel(textViewUpdatePromise);
		});
	}

	private FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
	}

	@Override
	public void run() {
	}

	private void setLoading() {
		textView.setText(R.string.lbl_loading);
	}
}
