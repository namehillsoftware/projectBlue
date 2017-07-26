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
import com.lasthopesoftware.messenger.promises.queued.cancellation.CancellationToken;
import com.vedsoft.futures.runnables.OneParameterAction;

import java.util.Map;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class FileNameTextViewSetter implements OneParameterAction<Messenger<Map<String, String>>> {

	private final TextView textView;
	private final Handler handler;
	private final ServiceFile serviceFile;

	public static Promise<Map<String, String>> startNew(ServiceFile serviceFile, TextView textView) {
		return new Promise<>(new FileNameTextViewSetter(textView, serviceFile));
	}

	private FileNameTextViewSetter(TextView textView, ServiceFile serviceFile) {
		this.textView = textView;
		this.handler = new Handler(textView.getContext().getMainLooper());
		this.serviceFile = serviceFile;
	}

	@Override
	public void runWith(Messenger<Map<String, String>> messenger) {
		handler.post(() -> 	textView.setText(R.string.lbl_loading));

		final CancellationToken cancellationToken = new CancellationToken();
		messenger.cancellationRequested(cancellationToken);

		if (cancellationToken.isCancelled()) return;

		final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
		final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
		final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

		if (cancellationToken.isCancelled()) return;

		final Promise<Map<String, String>> filePropertiesPromise = cachedFilePropertiesProvider.promiseFileProperties(serviceFile.getKey());
		filePropertiesPromise.error(runCarelessly(messenger::sendRejection));

		final CancellationProxy cancellationProxy = new CancellationProxy();
		messenger.cancellationRequested(cancellationProxy);
		cancellationProxy.doCancel(filePropertiesPromise);

		final Promise<Map<String, String>> textViewUpdatePromise =
			filePropertiesPromise.then(properties -> new DispatchedPromise<>(ct -> {
				final String fileName = properties.get(FilePropertiesProvider.NAME);

				if (!ct.isCancelled() && fileName != null)
					textView.setText(fileName);

				return properties;
			}, handler));

		textViewUpdatePromise
			.next(runCarelessly(messenger::sendResolution))
			.error(runCarelessly(messenger::sendRejection));

		cancellationProxy.doCancel(textViewUpdatePromise);
	}
}
