package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.GivenTwoMediaSources;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.MediaSourceQueue;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.FakeExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.FakeMediaSource;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.FakeTimeline;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WhenPlaybackCompletesForOne extends AndroidContext {

	private static final FakeMediaSource fakeMediaSource = new FakeMediaSource(
		new FakeTimeline(1),
		null,
		TrackGroupArray.EMPTY);
	private static final MediaSource mockMediaSourceTwo = mock(MediaSource.class);

	private static final EventHandlingExoPlayer exoPlayer = new EventHandlingExoPlayer(RuntimeEnvironment.systemContext.getMainLooper());
	private static final MediaSourceQueue mediaSourceQueue = new MediaSourceQueue();

	@Override
	public void before() throws Exception {
		final MediaSource promisedMediaSource = new FuturePromise<>(mediaSourceQueue.enqueueMediaSource(fakeMediaSource)).get();
		new FuturePromise<>(mediaSourceQueue.enqueueMediaSource(mockMediaSourceTwo)).get();

		final MediaSource.SourceInfoRefreshListener listener = (source, timeline, manifest) -> {};
		mediaSourceQueue.prepareSource(exoPlayer, true, listener);

		promisedMediaSource.releaseSource(listener);
	}

	@Test
	public void thenTheFirstMediaSourceIsPreparedCorrectly() {
		assertThat(fakeMediaSource.isPrepared()).isTrue();
	}

	@Test
	public void thenTheSecondMediaSourceIsPreparedCorrectly() {
		verify(mockMediaSourceTwo).prepareSource(argThat(a -> a == exoPlayer), booleanThat(a -> !a), any());
	}

	@Test
	public void thenTheMediaSourceQueueHasOneItemLeft() {
		assertThat(mediaSourceQueue.getSize()).isEqualTo(1);
	}

	private static class EventHandlingExoPlayer extends FakeExoPlayer
		implements Handler.Callback, PlayerMessage.Sender {

		private final Handler handler;

		EventHandlingExoPlayer(Looper looper) {
			this.handler = new Handler(looper, this);
		}

		@Override
		public void retry() {

		}

		@Override
		public PlayerMessage createMessage(PlayerMessage.Target target) {
			return new PlayerMessage(
				/* sender= */ this, target, Timeline.EMPTY, /* defaultWindowIndex= */ 0, handler);
		}

		@Override
		public SeekParameters getSeekParameters() {
			return null;
		}

		@Override
		public void sendMessage(PlayerMessage message) {
			handler.obtainMessage(0, message).sendToTarget();
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean handleMessage(Message msg) {
			PlayerMessage message = (PlayerMessage) msg.obj;
			try {
				message.getTarget().handleMessage(message.getType(), message.getPayload());
				message.markAsProcessed(/* isDelivered= */ true);
			} catch (ExoPlaybackException e) {
				fail("Unexpected ExoPlaybackException.");
			}
			return true;
		}

		@Nullable
		@Override
		public AudioComponent getAudioComponent() {
			return null;
		}

		@Nullable
		@Override
		public MetadataComponent getMetadataComponent() {
			return null;
		}

		@Override
		public Looper getApplicationLooper() {
			return null;
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public void previous() {

		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public void next() {

		}

		@Override
		public long getTotalBufferedDuration() {
			return 0;
		}

		@Override
		public long getContentDuration() {
			return 0;
		}

		@Override
		public long getContentBufferedPosition() {
			return 0;
		}
	}
}