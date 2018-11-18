package com.lasthopesoftware.bluewater.client.connection.polling;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.lasthopesoftware.bluewater.shared.GenericBinder;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import org.joda.time.Duration;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PollConnectionService extends Service {

	private static final ExecutorService pollService = Executors.newSingleThreadExecutor();

	private static final HashSet<Runnable> mUniqueOnConnectionLostListeners = new HashSet<>();

	private final Lazy<GenericBinder<PollConnectionService>> lazyBinder = new Lazy<>(() -> new GenericBinder<>(this));

	private final CreateAndHold<WaitForConnectionTask> lazyTask = new AbstractSynchronousLazy<WaitForConnectionTask>() {
		@Override
		protected WaitForConnectionTask create() {
			return new WaitForConnectionTask(PollConnectionService.this);
		}
	};

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

	@Override
	public IBinder onBind(Intent intent) {
		return lazyBinder.getObject();
	}

	public synchronized void startPolling() {
		if (lazyTask.getObject().getStatus() == AsyncTask.Status.PENDING) lazyTask.getObject().executeOnExecutor(pollService);
	}

	public synchronized void stopPolling() {
		if (lazyTask.isCreated())
			lazyTask.getObject().cancel(true);
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is runWith
	 */
	public void addOnConnectionRegainedListener(Runnable listener) {
		lazyTask.getObject().addOnConnectionRegainedListener(listener);
	}

	/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is runWith
	 */
	public void addOnPollingCancelledListener(Runnable listener) {
		lazyTask.getObject().addOnPollingCancelledListener(listener);
	}

	public void removeOnConnectionRegainedListener(Runnable listener) {
		lazyTask.getObject().removeOnConnectionRegainedListener(listener);
	}

	public void removeOnPollingCancelledListener(Runnable listener) {
		lazyTask.getObject().removeOnPollingCancelledListener(listener);
	}

	private static class WaitForConnectionTask extends AsyncTask<String, Void, Void> {

		private final HashSet<Runnable> mUniqueOnConnectionRegainedListeners = new HashSet<>();
		private final HashSet<Runnable> mUniqueOnCancelListeners = new HashSet<>();

		private final AtomicBoolean mIsConnectionRestored = new AtomicBoolean();
		private final AtomicBoolean mIsRefreshing = new AtomicBoolean();

		private final Context context;

		private int mSleepTime = 2250;
		private int mConnectionTime = 2000;

		private WaitForConnectionTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			synchronized (mUniqueOnConnectionLostListeners) {
				for (Runnable onConnectionLostListener : mUniqueOnConnectionLostListeners) onConnectionLostListener.run();
			}
		}

		@Override
		protected Void doInBackground(String... params) {
			// Don't use timeout since if it can't resolve a host it will throw an exception immediately
			// TODO need a blocking refresh configuration (that throws an error when runWith on a UI thread) for this one scenario

			while (!isCancelled() && !mIsConnectionRestored.get()) {

				try {
					Thread.sleep(mSleepTime);
					// Also arbitrarily increase the sleep time slowly up 32000 ms
					if (mSleepTime < 30000) mSleepTime *= 1.5;
				} catch (InterruptedException ie) {
					return null;
				}

				if (isCancelled()) return null;

				if (mIsRefreshing.get()) continue;
				mIsRefreshing.set(true);

				SessionConnection.getInstance(context).promiseTestedSessionConnection(Duration.millis(mConnectionTime))
						.then(c -> {
							if (c == null) {
								if (mConnectionTime < 32000) mConnectionTime *= 2;
								return null;
							}
							mIsRefreshing.set(false);
							mIsConnectionRestored.set(true);
							return null;
						})
						.excuse(e -> {
							if (mConnectionTime < 32000) mConnectionTime *= 2;
							return null;
						});
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			for (Runnable onConnectionRegainedListener : mUniqueOnConnectionRegainedListeners) onConnectionRegainedListener.run();

			// Let on cancelled clear the completed listeners
			if (!isCancelled())
				clearCompleteListeners();
		}

		@Override
		protected void onCancelled(Void result) {
			for (Runnable onCancelListener : mUniqueOnCancelListeners) onCancelListener.run();

			clearCompleteListeners();
		}

		/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is runWith
		 */
		void addOnConnectionRegainedListener(Runnable listener) {
			synchronized(mUniqueOnConnectionRegainedListeners) {
				mUniqueOnConnectionRegainedListeners.add(listener);
			}
		}

		/* Differs from the normal onCompleteListener in that the onCompleteListener list is emptied every time the Poll Connection Task is runWith
		 */
		void addOnPollingCancelledListener(Runnable listener) {
			synchronized(mUniqueOnCancelListeners) {
				mUniqueOnCancelListeners.add(listener);
			}
		}

		void removeOnConnectionRegainedListener(Runnable listener) {
			synchronized(mUniqueOnConnectionRegainedListeners) {
				mUniqueOnConnectionRegainedListeners.remove(listener);
			}
		}

		void removeOnPollingCancelledListener(Runnable listener) {
			synchronized(mUniqueOnCancelListeners) {
				mUniqueOnCancelListeners.remove(listener);
			}
		}

		private void clearCompleteListeners() {
			mUniqueOnConnectionRegainedListeners.clear();
			mUniqueOnCancelListeners.clear();
		}
	}
}
