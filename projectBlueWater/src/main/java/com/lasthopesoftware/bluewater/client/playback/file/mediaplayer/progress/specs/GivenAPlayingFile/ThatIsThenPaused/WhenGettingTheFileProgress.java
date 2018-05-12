package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.specs.GivenAPlayingFile.ThatIsThenPaused;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.MediaPlayerFileProgressReader;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static Duration progress;

	@BeforeClass
	public static void before() {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying())
			.thenReturn(true)
			.thenReturn(false);

		when(mockMediaPlayer.getCurrentPosition())
			.thenReturn(78)
			.thenReturn(new Random().nextInt());

		when(mockMediaPlayer.getDuration())
			.thenReturn(101);

		final MediaPlayerFileProgressReader mediaPlayerFileProgressReader = new MediaPlayerFileProgressReader(mockMediaPlayer);
		mediaPlayerFileProgressReader.getProgress();
		progress = mediaPlayerFileProgressReader.getProgress();
	}

	@Test
	public void thenTheFileProgressIsLastValidFileProgress() {
		assertThat(progress).isEqualTo(Duration.millis(78));
	}
}
