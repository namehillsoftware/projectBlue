package com.lasthopesoftware.bluewater.client.connection.helpers;

import android.content.Context;
import android.os.AsyncTask;
import com.lasthopesoftware.bluewater.client.connection.session.SessionConnection;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import org.joda.time.Duration;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PollConnection {
	
	private static final ExecutorService pollService = Executors.newSingleThreadExecutor(); 

	private final Context context;

	private final CreateAndHold<WaitForConnectionTask> lazyTask = new AbstractSynchronousLazy<WaitForConnectionTask>() {
		@Override
		protected WaitForConnectionTask create() {
			return new WaitForConnectionTask(context);
		}
	};
	
	private static final HashSet<Runnable> mUniqueOnConnectionLostListeners = new HashSet<>();
	
	private PollConnection(Context context) {
		this.context = context;
	}
	
	public synchronized void startPolling() {
		if (lazyTask.getObject().getStatus() == AsyncTask.Status.PENDING) lazyTask.getObject().executeOnExecutor(pollService);
	}
	
	public synchronized void stopPolling() {
		if (lazyTask.isCreated())
			lazyTask.getObject().cancel(true);
	}

	private synchronized boolean isFinished() {
		return lazyTask.isCreated() && lazyTask.getObject().getStatus() == AsyncTask.Status.FINISHED;
	}
	
	/* Differs from the normal on start listener in that it uses a static list that will be re-populated when a new Poll Connection task starts.
	 */
	public void addOnConnectionLostListener(Runnable listener) {
		synchronized(mUniqueOnConnectionLostListeners) {
			mUniqueOnConnectionLostListeners.add(listener);
		}
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

	public void removeOnConnectionLostListener(Runnable listener) {
		synchronized(mUniqueOnConnectionLostListeners) {
			mUniqueOnConnectionLostListeners.remove(listener);
		}
	}

	public void removeOnConnectionRegainedListener(Runnable listener) {
		lazyTask.getObject().removeOnConnectionRegainedListener(listener);
	}
	
	public void removeOnPollingCancelledListener(Runnable listener) {
		lazyTask.getObject().removeOnPollingCancelledListener(listener);
	}

	public static class Instance {
		private static PollConnection instance = null;
		
		public static synchronized PollConnection get(Context context) {
			if (instance == null || instance.isFinished()) instance = new PollConnection(context);
			return instance;
		}
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