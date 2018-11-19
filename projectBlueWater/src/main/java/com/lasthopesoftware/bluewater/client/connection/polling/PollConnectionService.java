package com.lasthopesoftware.bluewater.client.connection.polling;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import org.joda.time.Duration;

import java.util.HashSet;
import java.util.concurrent.CancellationException;

public class PollConnectionService extends Service {

	public static Promise<IConnectionProvider> pollSessionConnection(Context context) {
		return new Promise<PollConnectionService>(m -> context.bindService(new Intent(context, PollConnectionService.class), new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				m.sendResolution((PollConnectionService)(((GenericBinder<?>)service).getService()));
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
		}, BIND_AUTO_CREATE)).eventually(c -> c.lazyConnectionPoller.getObject());
	}

	private static final HashSet<Runnable> mUniqueOnConnectionLostListeners = new HashSet<>();

	/* Differs from the normal on start listener in that it uses a static list that will be re-populated when a new Poll Connection task starts.
	 */
	public static void addOnConnectionLostListener(Runnable listener) {
		synchronized(mUniqueOnConnectionLostListeners) {
			mUniqueOnConnectionLostListeners.add(listener);
		}
	}

	public static void removeOnConnectionLostListener(Runnable listener) {
		synchronized(mUniqueOnConnectionLostListeners) {
			mUniqueOnConnectionLostListeners.remove(listener);
		}
	}

	private final Lazy<GenericBinder<PollConnectionService>> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));

	private CreateAndHold<Promise<IConnectionProvider>> lazyConnectionPoller = new AbstractSynchronousLazy<Promise<IConnectionProvider>>() {
		@Override
		protected Promise<IConnectionProvider> create() {
			return new Promise<>(m -> {

				final CancellationToken cancellationToken = new CancellationToken();
				m.cancellationRequested(cancellationToken);

				pollSessionConnection(cancellationToken, 1000);
			});
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	private Promise<IConnectionProvider> pollSessionConnection(CancellationToken cancellationToken, int connectionTime) {
		if (cancellationToken.isCancelled()) {
			stopSelf();
			return new Promise<>(new CancellationException("Polling the session connection was cancelled"));
		}

		final int nextConnectionTime = connectionTime < 32000 ? connectionTime * 2 : connectionTime;
		return SessionConnection.getInstance(this)
			.promiseTestedSessionConnection(Duration.millis(connectionTime))
			.eventually(
				c -> {
					if (c == null) {
						return pollSessionConnection(cancellationToken, nextConnectionTime);
					}

					stopSelf();

					return new Promise<>(c);
				},
				e -> pollSessionConnection(cancellationToken, nextConnectionTime));
	}
}
