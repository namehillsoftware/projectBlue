package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.buffering;

import android.media.MediaPlayer;

import com.lasthopesoftware.promises.EmptyMessenger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by david on 10/23/16.
 */
public final class MediaPlayerBufferedPromise extends EmptyMessenger<IBufferingPlaybackHandler> {

	private static final Logger logger = LoggerFactory.getLogger(MediaPlayerBufferedPromise.class);

	private static final int bufferMax = 100;

	private final IBufferingPlaybackHandler bufferingPlaybackHandler;
	private final MediaPlayer mediaPlayer;

	private int bufferPercentage;
	private int lastBufferPercentage;

	public MediaPlayerBufferedPromise(IBufferingPlaybackHandler bufferingPlaybackHandler, MediaPlayer mediaPlayer) {
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
		this.mediaPlayer = mediaPlayer;
	}

	@Override
	public void requestResolution() {
		if (isBuffered()) {
			sendResolution(bufferingPlaybackHandler);
			return;
		}

		mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
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
		});
	}

	private boolean isBuffered() {
		if (lastBufferPercentage != bufferPercentage) {
			lastBufferPercentage = bufferPercentage;
			logger.info("Buffer percentage: " + String.valueOf(bufferPercentage) + "% Buffer Threshold: " + String.valueOf(bufferMax) + "%");
		}
		return lastBufferPercentage >= bufferMax;
	}
}
