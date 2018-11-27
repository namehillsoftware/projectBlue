package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.namehillsoftware.handoff.Messenger;
import com.namehillsoftware.handoff.promises.MessengerOperator;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BufferingExoPlayer
implements
	IBufferingPlaybackFile,
	MessengerOperator<IBufferingPlaybackFile>,
	MediaSourceEventListener
{
	private static final Logger logger = LoggerFactory.getLogger(BufferingExoPlayer.class);

	private final CreateAndHold<Promise<IBufferingPlaybackFile>> bufferingPlaybackFilePromise = new AbstractSynchronousLazy<Promise<IBufferingPlaybackFile>>() {
		@Override
		protected Promise<IBufferingPlaybackFile> create() {
			return new Promise<>((MessengerOperator<IBufferingPlaybackFile>) BufferingExoPlayer.this);
		}
	};

	private Messenger<IBufferingPlaybackFile> messenger;
	private boolean isTransferComplete;
	private Exception loadError;

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return bufferingPlaybackFilePromise.getObject();
	}

	@Override
	public void send(Messenger<IBufferingPlaybackFile> messenger) {
		this.messenger = messenger;
		if (isTransferComplete)
			messenger.sendResolution(this);
		if (loadError != null)
			messenger.sendRejection(loadError);
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
		isTransferComplete = true;
		if (messenger != null)
			messenger.sendResolution(this);
	}

	@Override
	public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {

	}

	@Override
	public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
		logger.error("An error occurred during playback buffering", error);
		loadError = error;
		if (messenger != null)
			messenger.sendRejection(error);
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
