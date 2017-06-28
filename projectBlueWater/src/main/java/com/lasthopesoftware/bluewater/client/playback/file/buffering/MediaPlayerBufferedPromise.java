package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import android.media.MediaPlayer;

import com.lasthopesoftware.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaPlayerBufferedPromise extends Promise<IBufferingPlaybackHandler> implements MediaPlayer.OnBufferingUpdateListener {

	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerBufferedPromise.class);

	private static final int bufferMax = 100;

	private final IBufferingPlaybackHandler bufferingPlaybackHandler;

	private int bufferPercentage;
	private int lastBufferPercentage;

	public MediaPlayerBufferedPromise(IBufferingPlaybackHandler bufferingPlaybackHandler, MediaPlayer mediaPlayer) {
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
		mediaPlayer.setOnBufferingUpdateListener(this);
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

		sendResolution(bufferingPlaybackHandler);
	}

	private boolean isBuffered() {
		if (lastBufferPercentage != bufferPercentage) {
			lastBufferPercentage = bufferPercentage;
			logger.info("Buffer percentage: " + String.valueOf(bufferPercentage) + "% Buffer Threshold: " + String.valueOf(bufferMax) + "%");
		}
		return lastBufferPercentage >= bufferMax;
	}
}
