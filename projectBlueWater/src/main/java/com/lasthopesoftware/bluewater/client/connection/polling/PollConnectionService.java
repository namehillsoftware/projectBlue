package com.lasthopesoftware.bluewater.client.connection.polling;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.Lazy;
import org.joda.time.Duration;

import java.util.HashSet;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class PollConnectionService extends Service {

	public static class Instance {

		public static Promise<PollConnectionService> promise(Context context) {
			return new Promise<>(m -> context.bindService(new Intent(context, PollConnectionService.class), new ServiceConnection() {

				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					m.sendResolution(((PollConnectionService)(((GenericBinder<?>)service).getService())));
				}

				@Override
				public void onServiceDisconnected(ComponentName name) {
				}
			}, BIND_AUTO_CREATE));
		}
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

	private final HashSet<Runnable> mUniqueOnConnectionRegainedListeners = new HashSet<>();
	private final HashSet<Runnable> mUniqueOnCancelListeners = new HashSet<>();

	private boolean isPolling;

	private boolean isCancelled;

	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	public void startPolling() {
		if (isPolling) return;
		isPolling = true;

		synchronized (mUniqueOnConnectionLostListeners) {
			for (Runnable onConnectionLostListener : mUniqueOnConnectionLostListeners) onConnectionLostListener.run();
		}

		pollSessionConnection(1000);
	}

	private void pollSessionConnection(int connectionTime) {
		if (isCancelled) {
			for (Runnable onCancelListener : mUniqueOnCancelListeners) onCancelListener.run();

			stopSelf();

			return;
		}

		final int nextConnectionTime = connectionTime < 32000 ? connectionTime * 2 : connectionTime;
		SessionConnection.getInstance(this)
			.promiseTestedSessionConnection(Duration.millis(connectionTime))
			.then(
				perform(c -> {
					if (c == null) {
						pollSessionConnection(nextConnectionTime);
						return;
					}

					for (Runnable onConnectionRegainedListener : mUniqueOnConnectionRegainedListeners) onConnectionRegainedListener.run();

					// Let on cancelled clear the completed listeners
					stopSelf();
				}),
				perform(e -> pollSessionConnection(nextConnectionTime)));
	}

	public synchronized void stopPolling() {
		isCancelled = true;
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is runWith
	 */
	public void addOnConnectionRegainedListener(Runnable listener) {
		synchronized(mUniqueOnConnectionRegainedListeners) {
			mUniqueOnConnectionRegainedListeners.add(listener);
		}
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is runWith
	 */
	public void addOnPollingCancelledListener(Runnable listener) {
		synchronized(mUniqueOnCancelListeners) {
			mUniqueOnCancelListeners.add(listener);
		}
	}

	public void removeOnConnectionRegainedListener(Runnable listener) {
		synchronized(mUniqueOnConnectionRegainedListeners) {
			mUniqueOnConnectionRegainedListeners.remove(listener);
		}
	}

	public void removeOnPollingCancelledListener(Runnable listener) {
		synchronized(mUniqueOnCancelListeners) {
			mUniqueOnCancelListeners.remove(listener);
		}
	}
}
