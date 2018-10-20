package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.specs.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsAtTheEnd;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenThePlayerWillNotPlayWhenReady {
	private static Player.EventListener eventListener;
	private static boolean isComplete;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(100L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);
		doAnswer((Answer<Void>) invocation -> {
			eventListener = invocation.getArgument(0);
			return null;
		}).when(mockExoPlayer).addListener(any());

		ExoPlayerPlaybackHandler exoPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		new FuturePromise<>(exoPlayerPlaybackHandler.promisePlayback().eventually(PlayingFile::promisePlayedFile)
			.then(p -> isComplete = true)).get(1, TimeUnit.SECONDS);

		eventListener.onPlayerStateChanged(false, Player.STATE_IDLE);
	}

	@Test
	public void thenPlaybackCompletes() {
		assertThat(isComplete).isTrue();
	}
}
