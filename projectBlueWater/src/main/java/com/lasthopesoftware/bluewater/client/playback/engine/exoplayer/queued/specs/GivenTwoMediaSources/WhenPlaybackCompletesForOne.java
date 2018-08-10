package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.GivenTwoMediaSources;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.upstream.Allocator;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.MediaSourceQueue;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.FakeExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.FakeMediaSource;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WhenPlaybackCompletesForOne extends AndroidContext {

	private static final FakeMediaSource fakeMediaSource = new FakeMediaSource(
		Timeline.EMPTY,
		null,
		TrackGroupArray.EMPTY);
	private static final MediaSource mockMediaSourceTwo = mock(MediaSource.class);

	private static final EventHandlingExoPlayer exoPlayer = new EventHandlingExoPlayer(RuntimeEnvironment.systemContext.getMainLooper());
	private static final MediaSourceQueue mediaSourceQueue = new MediaSourceQueue();

	@Override
	public void before() throws Exception {
		final MediaPeriod mediaPeriod = fakeMediaSource.createPeriod(
			new MediaSource.MediaPeriodId(0),
			null);
		new FuturePromise<>(mediaSourceQueue.enqueueMediaSource(fakeMediaSource)).get();
		new FuturePromise<>(mediaSourceQueue.enqueueMediaSource(mockMediaSourceTwo)).get();

		mediaSourceQueue.prepareSource(exoPlayer, true, (source, timeline, manifest) -> {});

		fakeMediaSource.releasePeriod(mediaPeriod);
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
		public PlayerMessage createMessage(PlayerMessage.Target target) {
			return new PlayerMessage(
				/* sender= */ this, target, Timeline.EMPTY, /* defaultWindowIndex= */ 0, handler);
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
	}
}