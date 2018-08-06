package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.GivenAMediaSource;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.MediaSourceQueue;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.specs.FakeExoPlayer;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WhenEnqueued extends AndroidContext {

	private static final MediaSource mockMediaSource = mock(MediaSource.class);

	private static final EventHandlingExoPlayer exoPlayer = new EventHandlingExoPlayer(RuntimeEnvironment.systemContext.getMainLooper());

	@Override
	public void before() throws Exception {
		final MediaSourceQueue mediaSourceQueue = new MediaSourceQueue();
		new FuturePromise<>(mediaSourceQueue.enqueueMediaSource(mockMediaSource)).get();

		mediaSourceQueue.prepareSource(exoPlayer, true, (source, timeline, manifest) -> {});
	}

	@Test
	public void thenItsPreparedCorrectly() {
		verify(mockMediaSource).prepareSource(argThat(a -> a == exoPlayer), booleanThat(a -> !a), any());
	}

	private static class EventHandlingExoPlayer extends FakeExoPlayer
		implements Handler.Callback, PlayerMessage.Sender {

		private final Handler handler;

		public EventHandlingExoPlayer(Looper looper) {
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