package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering;

import android.os.Handler;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BufferingExoPlayer
extends Promise<IBufferingPlaybackFile>
implements
	IBufferingPlaybackFile,
	MediaSourceEventListener
{
	private static final Logger logger = LoggerFactory.getLogger(BufferingExoPlayer.class);
	private final MediaSource mediaSource;

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return this;
	}

	public BufferingExoPlayer(Handler handler, MediaSource mediaSource) {
		this.mediaSource = mediaSource;
		mediaSource.addEventListener(handler, this);
	}

	@Override
	public void onMediaPeriodCreated(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

	}

	@Override
	public void onMediaPeriodReleased(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

	}

	@Override
	public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

	}

	@Override
	public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
		resolve(this);
		mediaSource.removeEventListener(this);
	}

	@Override
	public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

	}

	@Override
	public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
		logger.error("An error occurred during playback buffering", error);
		reject(error);
		mediaSource.removeEventListener(this);
	}

	@Override
	public void onReadingStarted(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId) {

	}

	@Override
	public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {

	}

	@Override
	public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {

	}
}
