package com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.specs.GivenAPlayingFile;

import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.playback.file.mediaplayer.progress.MediaPlayerFileProgressReader;
import com.lasthopesoftware.bluewater.client.playback.file.progress.FileProgress;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WhenGettingTheFileProgress {

	private static FileProgress progress;

	@BeforeClass
	public static void before() {
		final MediaPlayer mockMediaPlayer = mock(MediaPlayer.class);
		when(mockMediaPlayer.isPlaying()).thenReturn(true);
		when(mockMediaPlayer.getCurrentPosition()).thenReturn(75);
		when(mockMediaPlayer.getDuration()).thenReturn(101);

		final MediaPlayerFileProgressReader mediaPlayerFileProgressReader = new MediaPlayerFileProgressReader(mockMediaPlayer);
		progress = mediaPlayerFileProgressReader.getFileProgress();
	}

	@Test
	public void thenTheFileProgressIsCorrect() {
		assertThat(progress).isEqualTo(new FileProgress(75, 101));
	}
}
