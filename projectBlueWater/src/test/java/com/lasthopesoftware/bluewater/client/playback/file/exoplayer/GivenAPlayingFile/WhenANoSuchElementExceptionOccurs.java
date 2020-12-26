package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenANoSuchElementExceptionOccurs {

	private static ExoPlayerException exoPlayerException;
	private static Player.EventListener eventListener;
	private static Boolean isComplete;

	@BeforeClass
	public static void before() throws InterruptedException, TimeoutException, ExecutionException {
		final ExoPlayer mockExoPlayer = mock(ExoPlayer.class);
		when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
		when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
		when(mockExoPlayer.getDuration()).thenReturn(100L);
		doAnswer((Answer<Void>) invocation -> {
			eventListener = invocation.getArgument(0);
			return null;
		}).when(mockExoPlayer).addListener(any());

		final ExoPlayerPlaybackHandler exoPlayerPlaybackHandlerPlayerPlaybackHandler = new ExoPlayerPlaybackHandler(mockExoPlayer);
		final FuturePromise<Boolean> promisedFuture = new FuturePromise<>(exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
			.eventually(PlayingFile::promisePlayedFile)
			.then(p -> true, e -> false));

		eventListener.onPlayerError(ExoPlaybackException.createForUnexpected(new NoSuchElementException()));

		try {
			isComplete = promisedFuture.get(1, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ExoPlayerException) {
				exoPlayerException = (ExoPlayerException) e.getCause();
				return;
			}

			throw e;
		}
	}

	@Test
	public void thenPlaybackCompletes() {
		assertThat(isComplete).isTrue();
	}

	@Test
	public void thenNoPlaybackErrorOccurs() {
		assertThat(exoPlayerException).isNull();
	}
}
