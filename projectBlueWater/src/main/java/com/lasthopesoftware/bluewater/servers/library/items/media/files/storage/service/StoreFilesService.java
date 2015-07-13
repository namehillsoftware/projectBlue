package com.lasthopesoftware.bluewater.servers.library.items.media.files.storage.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.lasthopesoftware.bluewater.disk.sqlite.objects.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.threading.SimpleTask;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class StoreFilesService extends Service {

	private static class QueuedFileHolder {
		public final IFile file;
		public final String remotePath;

		private QueuedFileHolder(IFile file, String remotePath) {
			this.file = file;
			this.remotePath = remotePath;
		}
	}

	private static final String queueFileForDownload = StoreFilesService.class.getCanonicalName() + ".queueFileForDownload";

	private static final String destinationPathKey = StoreFilesService.class.getCanonicalName() + ".destinationPathKey";
	private static final String fileIdKey = StoreFilesService.class.getCanonicalName() + ".fileIdKey";

	private final Queue<QueuedFileHolder> mFileDownloadQueue = new ArrayDeque<>();
	private final Set<Integer> mQueuedFileKeys = new HashSet<>();

	private static SimpleTask<Void, Void, Void> mFileDownloadTask;

	public static void queueFileForDownload(Context context, IFile file, StoredFile storedFile) {
		final Intent intent = new Intent(context, StoreFilesService.class);
		intent.setAction(queueFileForDownload);
		intent.putExtra(destinationPathKey, storedFile.getPath());
		intent.putExtra(fileIdKey, file.getKey());
		context.startService(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final int returnValue = START_NOT_STICKY;

		if (!intent.getAction().equals(queueFileForDownload)) return returnValue;

		final int fileKey = intent.getIntExtra(fileIdKey, -1);
		if (fileKey == -1) return returnValue;

		final String remotePath = intent.getStringExtra(destinationPathKey);
		if (remotePath == null) return returnValue;

		queueAndStartDownloading(fileKey, remotePath);

		return returnValue;
	}

	private void queueAndStartDownloading(int fileKey, String remotePath) {
		if (mQueuedFileKeys.add(fileKey))
			mFileDownloadQueue.add(new QueuedFileHolder(new File(fileKey), remotePath));
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public class StoreMusicServiceBinder extends Binder {
		StoreFilesService getService() {
            return StoreFilesService.this;
        }
    }
	
	private final IBinder mBinder = new StoreMusicServiceBinder();
}
