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
import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.Promise;
import com.lasthopesoftware.messenger.promises.propagation.CancellationProxy;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileNameTextViewSetter implements OneParameterAction<Messenger<Map<String, String>>> {

	private static final Lock lock = new ReentrantLock();

	private final TextView textView;
	private final Handler handler;
	private final ServiceFile serviceFile;

	public static Promise<Map<String, String>> startNew(ServiceFile serviceFile, TextView textView) {
		lock.lock();

		textView.setText(R.string.lbl_loading);
		final Promise<Map<String, String>> returnPromise = new Promise<>(new FileNameTextViewSetter(textView, serviceFile));

		returnPromise
			.then(m -> {
				lock.unlock();
				return null;
			})
			.excuse(r -> {
				lock.unlock();
				return null;
			});

		return returnPromise;
	}

	private FileNameTextViewSetter(TextView textView, ServiceFile serviceFile) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
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

		final Promise<Map<String, String>> textViewUpdatePromise =
			filePropertiesPromise.eventually(properties -> new DispatchedPromise<>(ct -> {
				final String fileName = properties.get(FilePropertiesProvider.NAME);

				if (fileName != null && !ct.isCancelled())
					textView.setText(fileName);

				return properties;
			}, handler));

		cancellationProxy.doCancel(textViewUpdatePromise);

		textViewUpdatePromise
			.then(props -> {
				messenger.sendResolution(props);
				return null;
			})
			.excuse(e -> {
				messenger.sendRejection(e);
				return null;
			});
	}
}
