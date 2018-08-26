package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.specs.GivenATypicalExoPlayer.WithMultipleTracks;

import android.os.Looper;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlayerMessage;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.ActiveExoPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class WhenObservingPlayback {
	private static final PlaybackReportingExoPlayer exoPlayer = new PlaybackReportingExoPlayer();
	private static List<PositionedPlayingFile> playingFiles = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final ActiveExoPlaylistPlayer exoPlaylistPlayer = new ActiveExoPlaylistPlayer(exoPlayer);
		exoPlaylistPlayer.observe().subscribe(playingFiles::add);
		exoPlayer.broadcastTrackChanged();
		exoPlayer.broadcastTrackChanged();
		exoPlayer.broadcastTrackChanged();
		exoPlayer.broadcastTrackChanged();
	}

	@Test
	public void thenFourPlayingFilesAreObserved() {
		assertThat(playingFiles.size()).isEqualTo(4);
	}

	private static class PlaybackReportingExoPlayer implements ExoPlayer {

		private final List<Player.EventListener> listeners = new ArrayList<>();
		private boolean playWhenReady;

		@Override
		public final void setPlayWhenReady(boolean playWhenReady) {
			this.playWhenReady = playWhenReady;
		}

		@Override
		public final boolean getPlayWhenReady() {
			return playWhenReady;
		}

		@Override
		public void setRepeatMode(int repeatMode) {

		}

		@Override
		public int getRepeatMode() {
			return 0;
		}

		@Override
		public void setShuffleModeEnabled(boolean shuffleModeEnabled) {

		}

		@Override
		public boolean getShuffleModeEnabled() {
			return false;
		}

		@Override
		public boolean isLoading() {
			return false;
		}

		@Override
		public void seekToDefaultPosition() {

		}

		@Override
		public void seekToDefaultPosition(int windowIndex) {

		}

		@Override
		public void seekTo(long positionMs) {

		}

		@Override
		public void seekTo(int windowIndex, long positionMs) {

		}

		@Override
		public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters) {

		}

		@Override
		public PlaybackParameters getPlaybackParameters() {
			return null;
		}

		@Override
		public void stop() {

		}

		@Override
		public void stop(boolean reset) {

		}

		@Override
		public void release() {

		}

		@Override
		public int getRendererCount() {
			return 0;
		}

		@Nullable
		@Override
		public int getRendererType(int index) {
			return 0;
		}

		@Override
		public TrackGroupArray getCurrentTrackGroups() {
			return null;
		}

		@Override
		public TrackSelectionArray getCurrentTrackSelections() {
			return null;
		}

		@Nullable
		@Override
		public Object getCurrentManifest() {
			return null;
		}

		@Override
		public Timeline getCurrentTimeline() {
			return null;
		}

		@Override
		public int getCurrentPeriodIndex() {
			return 0;
		}

		@Override
		public int getCurrentWindowIndex() {
			return 0;
		}

		@Override
		public int getNextWindowIndex() {
			return 0;
		}

		@Override
		public int getPreviousWindowIndex() {
			return 0;
		}

		@Nullable
		@Override
		public Object getCurrentTag() {
			return null;
		}

		@Override
		public long getDuration() {
			return 0;
		}

		@Override
		public long getCurrentPosition() {
			return 0;
		}

		@Override
		public long getBufferedPosition() {
			return 0;
		}

		@Override
		public int getBufferedPercentage() {
			return 0;
		}

		@Override
		public boolean isCurrentWindowDynamic() {
			return false;
		}

		@Override
		public boolean isCurrentWindowSeekable() {
			return false;
		}

		@Override
		public boolean isPlayingAd() {
			return false;
		}

		@Override
		public int getCurrentAdGroupIndex() {
			return 0;
		}

		@Override
		public int getCurrentAdIndexInAdGroup() {
			return 0;
		}

		@Override
		public long getContentPosition() {
			return 0;
		}

		@Nullable
		@Override
		public VideoComponent getVideoComponent() {
			return null;
		}

		@Nullable
		@Override
		public TextComponent getTextComponent() {
			return null;
		}

		@Override
		public final void addListener(Player.EventListener listener) {
			listeners.add(listener);
		}

		@Override
		public void removeListener(Player.EventListener listener) {

		}

		@Override
		public int getPlaybackState() {
			return 0;
		}

		@Nullable
		@Override
		public ExoPlaybackException getPlaybackError() {
			return null;
		}

		final void broadcastTrackChanged() {
			Stream.of(listeners)
				.forEach(l -> l.onPositionDiscontinuity(Player.DISCONTINUITY_REASON_PERIOD_TRANSITION));
		}

		@Override
		public Looper getPlaybackLooper() {
			return null;
		}

		@Override
		public void prepare(MediaSource mediaSource) {

		}

		@Override
		public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {

		}

		@Override
		public PlayerMessage createMessage(PlayerMessage.Target target) {
			return null;
		}

		@Override
		public void sendMessages(ExoPlayerMessage... messages) {

		}

		@Override
		public void blockingSendMessages(ExoPlayerMessage... messages) {

		}

		@Override
		public void setSeekParameters(@Nullable SeekParameters seekParameters) {

		}
	}
}
