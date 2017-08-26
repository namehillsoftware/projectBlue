package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import android.media.MediaPlayer;

import com.lasthopesoftware.messenger.Messenger;
import com.lasthopesoftware.messenger.promises.MessengerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaPlayerBufferedPromise implements MessengerTask<IBufferingPlaybackHandler>, MediaPlayer.OnBufferingUpdateListener {

	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerBufferedPromise.class);

	private static final int bufferMax = 100;

	private final IBufferingPlaybackHandler bufferingPlaybackHandler;

	private volatile Messenger<IBufferingPlaybackHandler> messenger;

	private int bufferPercentage;
	private int lastBufferPercentage;

	public MediaPlayerBufferedPromise(IBufferingPlaybackHandler bufferingPlaybackHandler, MediaPlayer mediaPlayer) {
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
		mediaPlayer.setOnBufferingUpdateListener(this);
	}

	@Override
	public void execute(Messenger<IBufferingPlaybackHandler> messenger) {
		this.messenger = messenger;
		if (isBuffered())
			messenger.sendResolution(bufferingPlaybackHandler);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// Handle weird exceptional behavior seen online http://stackoverflow.com/questions/21925454/android-mediaplayer-onbufferingupdatelistener-percentage-of-buffered-content-i
		if (percent < 0 || percent > 100) {
			logger.warn("Buffering percentage was bad: " + String.valueOf(percent));
			percent = (int) Math.round((((Math.abs(percent) - 1) * 100.0 / Integer.MAX_VALUE)));
		}

		bufferPercentage = percent;

		if (!isBuffered()) return;

		// remove the listener
		mp.setOnBufferingUpdateListener(null);

		if (messenger != null)
			messenger.sendResolution(bufferingPlaybackHandler);
	}

	private boolean isBuffered() {
		if (lastBufferPercentage != bufferPercentage) {
			lastBufferPercentage = bufferPercentage;
			logger.info("Buffer percentage: " + String.valueOf(bufferPercentage) + "% Buffer Threshold: " + String.valueOf(bufferMax) + "%");
		}
		return lastBufferPercentage >= bufferMax;
	}
}
