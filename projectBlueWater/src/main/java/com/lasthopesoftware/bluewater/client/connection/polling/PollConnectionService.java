package com.lasthopesoftware.bluewater.client.connection.polling;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.handoff.promises.response.VoidResponse;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.util.HashSet;
import java.util.concurrent.CancellationException;

public class PollConnectionService extends Service implements MessengerOperator<IConnectionProvider> {

	public static Promise<IConnectionProvider> pollSessionConnection(Context context) {
		final Promise<PollConnectionServiceConnectionHolder> promiseConnectedService =
			new Promise<>(m -> context.bindService(new Intent(context, PollConnectionService.class), new ServiceConnection() {

				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					m.sendResolution(
						new PollConnectionServiceConnectionHolder(
							(PollConnectionService) (((GenericBinder<?>) service).getService()),
							this));
				}

				@Override
				public void onServiceDisconnected(ComponentName name) {}
			}, BIND_AUTO_CREATE));

		return promiseConnectedService
			.eventually(s ->
				s.pollConnectionService.lazyConnectionPoller.getObject()
					.must(() -> context.unbindService(s.serviceConnection)));
	}

	private static final HashSet<Runnable> uniqueOnConnectionLostListeners = new HashSet<>();

	/* Differs from the normal on start listener in that it uses a static list that will be re-populated when a new Poll Connection task starts.
	 */
	public static void addOnConnectionLostListener(Runnable listener) {
		synchronized(uniqueOnConnectionLostListeners) {
			uniqueOnConnectionLostListeners.add(listener);
		}
	}

	public static void removeOnConnectionLostListener(Runnable listener) {
		synchronized(uniqueOnConnectionLostListeners) {
			uniqueOnConnectionLostListeners.remove(listener);
		}
	}

	private final Lazy<GenericBinder<PollConnectionService>> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));

	private final Lazy<Handler> lazyHandler = new Lazy<>(() -> new Handler(getMainLooper()));

	private final CreateAndHold<Promise<IConnectionProvider>> lazyConnectionPoller = new AbstractSynchronousLazy<Promise<IConnectionProvider>>() {
		@Override
		protected Promise<IConnectionProvider> create() {
			for (final Runnable connectionLostListener : uniqueOnConnectionLostListeners)
				connectionLostListener.run();

			return new Promise<>(PollConnectionService.this);
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	@Override
	public void send(Messenger<IConnectionProvider> messenger) {
		final CancellationToken cancellationToken = new CancellationToken();
		messenger.cancellationRequested(cancellationToken);

		pollSessionConnection(messenger, cancellationToken, 1000);
	}

	private void pollSessionConnection(Messenger<IConnectionProvider> messenger, CancellationToken cancellationToken, int connectionTime) {
		if (cancellationToken.isCancelled()) {
			messenger.sendRejection(new CancellationException("Polling the session connection was cancelled"));
			return;
		}

		final int nextConnectionTime = connectionTime < 32000 ? connectionTime * 2 : connectionTime;
		SessionConnection.getInstance(this)
			.promiseTestedSessionConnection()
			.then(
				new VoidResponse<>(c -> {
					if (c != null) {
						messenger.sendResolution(c);
						return;
					}

					lazyHandler.getObject()
						.postDelayed(() -> pollSessionConnection(messenger, cancellationToken, nextConnectionTime),
							connectionTime);
				}),
				new VoidResponse<>(e -> lazyHandler.getObject()
					.postDelayed(() -> pollSessionConnection(messenger, cancellationToken, nextConnectionTime),
						connectionTime)));
	}

	private static class PollConnectionServiceConnectionHolder {

		final PollConnectionService pollConnectionService;
		final ServiceConnection serviceConnection;

		private PollConnectionServiceConnectionHolder(PollConnectionService pollConnectionService, ServiceConnection serviceConnection) {
			this.pollConnectionService = pollConnectionService;
			this.serviceConnection = serviceConnection;
		}
	}
}
