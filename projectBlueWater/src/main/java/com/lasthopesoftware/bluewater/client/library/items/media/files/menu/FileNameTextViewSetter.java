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
import com.lasthopesoftware.messenger.promises.queued.QueuedPromise;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FileNameTextViewSetter implements OneParameterAction<Messenger<Map<String, String>>> {

	private static final Executor executor = Executors.newSingleThreadExecutor();

	private final TextView textView;
	private final Handler handler;
	private final ServiceFile serviceFile;

	public static Promise<Map<String, String>> startNew(ServiceFile serviceFile, TextView textView) {
		textView.setText(R.string.lbl_loading);
		return new QueuedPromise<>(new FileNameTextViewSetter(textView, serviceFile), executor);
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

		if (cancellationProxy.isCancelled()) return;

		final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
		final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

		if (cancellationProxy.isCancelled()) return;

		final Promise<Map<String, String>> filePropertiesPromise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());

		cancellationProxy.doCancel(filePropertiesPromise);

		final Promise<Map<String, String>> textViewUpdatePromise =
			filePropertiesPromise.then(properties -> new DispatchedPromise<>(ct -> {
				final String fileName = properties.get(FilePropertiesProvider.NAME);

				if (fileName != null && !ct.isCancelled())
					textView.setText(fileName);

				return properties;
			}, handler));

		cancellationProxy.doCancel(textViewUpdatePromise);

		final CountDownLatch countDownLatch = new CountDownLatch(1);

		textViewUpdatePromise
			.next(props -> {
				if (cancellationProxy.isCancelled())
					textView.setText(R.string.lbl_loading);

				messenger.sendResolution(props);
				countDownLatch.countDown();
				return null;
			})
			.error(e -> {
				messenger.sendRejection(e);
				countDownLatch.countDown();
				return null;
			});

		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			messenger.sendRejection(e);
		}
	}
}
