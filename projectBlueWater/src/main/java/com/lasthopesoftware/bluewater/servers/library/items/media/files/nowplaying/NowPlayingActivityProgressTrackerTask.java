package com.lasthopesoftware.bluewater.servers.library.items.media.files.nowplaying;

import android.os.AsyncTask;
import android.os.Message;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.vedsoft.lazyj.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NowPlayingActivityProgressTrackerTask extends AsyncTask<Void, Void, Void> {
	private static final Logger logger = LoggerFactory.getLogger(NowPlayingActivityProgressTrackerTask.class);
	private static final ExecutorService trackerExecutor = Executors.newSingleThreadExecutor();

	private final IPlaybackFile filePlayer;

	private final NowPlayingActivityMessageHandler handler;

	private final Lazy<Integer> fileDuration = new Lazy<Integer>() {
		@Override
		protected Integer initialize() {
			try {
				return filePlayer.getDuration();
			} catch (IOException e) {
				logger.error("There was an error getting file duration");
				return -1;
			}
		}
	};

	public static NowPlayingActivityProgressTrackerTask trackProgress(IPlaybackFile filePlayer, NowPlayingActivityMessageHandler handler) {
		final NowPlayingActivityProgressTrackerTask newProgressTrackerThread = new NowPlayingActivityProgressTrackerTask(filePlayer, handler);
		newProgressTrackerThread.executeOnExecutor(trackerExecutor);
		return newProgressTrackerThread;
	}
	
	private NowPlayingActivityProgressTrackerTask(IPlaybackFile filePlayer, NowPlayingActivityMessageHandler handler) {
		this.filePlayer = filePlayer;
		this.handler = handler;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		while (!isCancelled() && filePlayer != null && filePlayer.isMediaPlayerCreated()) {
			try {
				
				if (filePlayer.isPlaying())
					handler.sendMessage(getUpdatePlayingMessage());
				
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return null;
			}
		}
		
		return null;
	}
	
	private Message getUpdatePlayingMessage() {
		final Message msg = new Message();
		msg.what = NowPlayingActivityMessageHandler.UPDATE_PLAYING;
		msg.arg1 = filePlayer.getCurrentPosition();
		msg.arg2 = fileDuration.getObject();
		
		return msg;
	}
}