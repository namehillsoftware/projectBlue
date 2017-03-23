package com.lasthopesoftware.bluewater.client.library.items.media.files.menu;

import android.os.Handler;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.repository.FilePropertyCache;
import com.lasthopesoftware.bluewater.shared.promises.resolutions.Dispatch;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.IRejectedPromise;
import com.lasthopesoftware.promises.IResolvedPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;
import com.vedsoft.futures.runnables.ThreeParameterAction;

import java.util.Map;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

/**
 * Created by david on 4/14/15.
 */
public class FileNameTextViewSetter implements CarelessTwoParameterFunction<Map<String, String>, OneParameterAction<Runnable>, Void>, Runnable {

	private final TextView textView;
	private boolean isCancelled;

	public static IPromise<Map<String, String>> startNew(IFile file, TextView textView) {
		final FileNameTextViewSetter fileNameTextViewSetter = new FileNameTextViewSetter(textView);
		final Handler handler = new Handler(textView.getContext().getMainLooper());

		fileNameTextViewSetter.setLoading();

		return new Promise<>(new ThreeParameterAction<IResolvedPromise<Map<String, String>>, IRejectedPromise, OneParameterAction<Runnable>>() {
			boolean isCancelled;

			@Override
			public void runWith(IResolvedPromise<Map<String, String>> resolve, IRejectedPromise reject, OneParameterAction<Runnable> onCancelled) {
				onCancelled.runWith(() -> isCancelled = true);

				if (isCancelled) return;

				final IConnectionProvider connectionProvider = SessionConnection.getSessionConnectionProvider();
				final FilePropertyCache filePropertyCache = FilePropertyCache.getInstance();
				final CachedFilePropertiesProvider cachedFilePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, filePropertyCache, new FilePropertiesProvider(connectionProvider, filePropertyCache));

				final IPromise<Map<String, String>> promise = cachedFilePropertiesProvider.promiseFileProperties(file.getKey());
				promise.then(runCarelessly(resolve::withResult));
				promise.error(runCarelessly(reject::withError));

				final IPromise<Void> textViewUpdatePromise = promise.then(Dispatch.toHandler(fileNameTextViewSetter, handler));

				onCancelled.runWith(() -> {
					isCancelled = true;
					promise.cancel();
					textViewUpdatePromise.cancel();
				});
			}
		});
	}

	private FileNameTextViewSetter(TextView textView) {
		this.textView = textView;
	}

	@Override
	public void run() {
		isCancelled = true;
	}

	private void setLoading() {
		textView.setText(R.string.lbl_loading);
	}

	@Override
	public Void resultFrom(Map<String, String> properties, OneParameterAction<Runnable> onCancelled) {
		onCancelled.runWith(this);

		final String fileName = properties.get(FilePropertiesProvider.NAME);

		if (!isCancelled && fileName != null)
			textView.setText(fileName);

		return null;
	}
}
