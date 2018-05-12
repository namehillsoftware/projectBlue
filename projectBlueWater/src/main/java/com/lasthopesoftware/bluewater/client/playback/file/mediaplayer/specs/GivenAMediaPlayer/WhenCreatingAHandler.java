package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.specs.GivenAMediaPlayer;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.MediaPlayerPlaybackHandler;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenCreatingAHandler {

	private static MediaPlayer.OnCompletionListener onCompletionListener;
	private static MediaPlayer.OnErrorListener onErrorListener;
	private static MediaPlayer.OnInfoListener onInfoListener;

	@BeforeClass
	public static void before() {
		final MediaPlayer mediaPlayer = new MediaPlayer() {
			@Override
			public void setOnCompletionListener(OnCompletionListener listener) {
				onCompletionListener = listener;
			}

			@Override
			public void setOnErrorListener(OnErrorListener listener) {
				onErrorListener = listener;
			}

			@Override
			public void setOnInfoListener(OnInfoListener listener) {
				onInfoListener = listener;
			}
		};

		new MediaPlayerPlaybackHandler(mediaPlayer);
	}

	@Test
	public void thenTheOnCompletionHandlerIsSet() {
		assertThat(onCompletionListener).isNotNull();
	}

	@Test
	public void thenTheOnErrorHandlerIsSet() {
		assertThat(onErrorListener).isNotNull();
	}

	@Test
	public void thenTheOnInfoHandlerIsSet() {
		assertThat(onInfoListener).isNotNull();
	}
}
