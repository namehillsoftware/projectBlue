package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import android.media.MediaPlayer;

import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BufferingMediaPlayerTask implements MessengerOperator<IBufferingPlaybackFile>, MediaPlayer.OnBufferingUpdateListener {

	private static final Logger logger = LoggerFactory.getLogger(BufferingMediaPlayerTask.class);

	private static final int bufferMax = 100;

	private final IBufferingPlaybackFile bufferingPlaybackHandler;

	private volatile Messenger<IBufferingPlaybackFile> messenger;

	private int bufferPercentage;
	private int lastBufferPercentage;

	BufferingMediaPlayerTask(IBufferingPlaybackFile bufferingPlaybackHandler, MediaPlayer mediaPlayer) {
		this.bufferingPlaybackHandler = bufferingPlaybackHandler;
		mediaPlayer.setOnBufferingUpdateListener(this);
	}

	@Override
	public void send(Messenger<IBufferingPlaybackFile> messenger) {
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
