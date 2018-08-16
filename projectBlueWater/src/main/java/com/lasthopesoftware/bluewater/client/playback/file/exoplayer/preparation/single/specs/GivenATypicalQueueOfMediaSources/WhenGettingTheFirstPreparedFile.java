package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.single.specs.GivenATypicalQueueOfMediaSources;

import android.net.Uri;
import android.os.Handler;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued.MediaSourceQueue;
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.single.SingleExoPlayerPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.PreparedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.volume.ManagePlayableFileVolume;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.lasthopesoftware.specs.AndroidContext;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class WhenGettingTheFirstPreparedFile extends AndroidContext {

	private static final List<Player.EventListener> eventListener = new CopyOnWriteArrayList<>();
	private static PreparedPlayableFile preparedPlayableFile;

	public void before() throws ExecutionException, InterruptedException {
		final ExoPlayer exoPlayer = mock(ExoPlayer.class);
		doAnswer((Answer<Void>) invocation -> {
			eventListener.add(invocation.getArgument(0));
			return null;
		}).when(exoPlayer).addListener(any());

		final SingleExoPlayerPlaybackPreparer exoPlayerPlaybackPreparer =
			new SingleExoPlayerPlaybackPreparer(
				exoPlayer,
				uri -> new ExtractorMediaSource.Factory(mock(DataSource.Factory.class)),
				new MediaSourceQueue(),
				mock(ManagePlayableFileVolume.class),
				new Handler(),
				(sf) -> new Promise<>(Uri.EMPTY));

		final FuturePromise<PreparedPlayableFile> preparedPlayableFilePromise = new FuturePromise<>(exoPlayerPlaybackPreparer.promisePreparedPlaybackFile(
			new ServiceFile(4),
			0));

		Stream.of(eventListener).forEach(e -> e.onPlayerStateChanged(true, Player.STATE_READY));

		preparedPlayableFile = preparedPlayableFilePromise.get();
	}

	@Test
	public void thenTheFileIsPrepared() {
		assertThat(preparedPlayableFile).isNotNull();
	}
}
