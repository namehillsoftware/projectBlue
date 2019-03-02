package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued;

import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.namehillsoftware.handoff.promises.Promise;
import com.vedsoft.futures.callables.Function;

import java.io.IOException;

public class MediaSourceQueue implements MediaSource, QueueMediaSources {

	private final ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();

	private volatile Promise<?> activePromise;

	@Override
	public final Promise<MediaSource> enqueueMediaSource(MediaSource mediaSource) {
		return queuePromise(() -> new Promise<>(m -> {
			final MediaSourceHolder holder = new MediaSourceHolder(mediaSource);
			concatenatingMediaSource.addMediaSource(holder, () -> m.sendResolution(holder));
		}));
	}

	private Promise<Void> removeMediaSource(MediaSource mediaSource) {
		return queuePromise(() -> {
			for (int i = 0; i < concatenatingMediaSource.getSize(); i++) {
				final MediaSource indexedMediaSource = concatenatingMediaSource.getMediaSource(i);
				if (indexedMediaSource != mediaSource) continue;

				final int removalIndex = i;
				return new Promise<>(m -> concatenatingMediaSource.removeMediaSource(removalIndex, () -> m.sendResolution(null)));
			}

			return Promise.empty();
		});
	}

	private synchronized <T> Promise<T> queuePromise(Function<Promise<T>> promiseProducer) {
		final Promise<T> returnPromise = activePromise == null
			? promiseProducer.result()
			: activePromise.eventually(o -> promiseProducer.result());

		activePromise = returnPromise;
		return returnPromise;
	}

	public final int getSize() {
		return concatenatingMediaSource.getSize();
	}

	@Override
	public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
		concatenatingMediaSource.addEventListener(handler, eventListener);
	}

	@Override
	public void removeEventListener(MediaSourceEventListener eventListener) {
		concatenatingMediaSource.removeEventListener(eventListener);
	}

	@Nullable
	@Override
	public Object getTag() {
		return concatenatingMediaSource.getTag();
	}

	@Override
	public void prepareSource(ExoPlayer player, boolean isTopLevelSource, SourceInfoRefreshListener listener) {
		concatenatingMediaSource.prepareSource(player, isTopLevelSource, listener);
	}

	@Override
	public void prepareSource(ExoPlayer player, boolean isTopLevelSource, SourceInfoRefreshListener listener, @Nullable TransferListener mediaTransferListener) {
		concatenatingMediaSource.prepareSource(player, isTopLevelSource, listener, mediaTransferListener);
	}

	@Override
	public void maybeThrowSourceInfoRefreshError() throws IOException {
		concatenatingMediaSource.maybeThrowSourceInfoRefreshError();
	}

	@Override
	public final MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
		return concatenatingMediaSource.createPeriod(id, allocator);
	}

	@Override
	public final void releasePeriod(MediaPeriod mediaPeriod) {
		concatenatingMediaSource.releasePeriod(mediaPeriod);
	}

	@Override
	public void releaseSource(SourceInfoRefreshListener listener) {
		concatenatingMediaSource.releaseSource(listener);
	}

	private final class MediaSourceHolder implements MediaSource {

		private final MediaSource mediaSource;

		MediaSourceHolder(MediaSource mediaSource) {
			this.mediaSource = mediaSource;
		}

		@Override
		public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
			this.mediaSource.addEventListener(handler, eventListener);
		}

		@Override
		public void removeEventListener(MediaSourceEventListener eventListener) {
			this.mediaSource.removeEventListener(eventListener);
		}

		@Nullable
		@Override
		public Object getTag() {
			return mediaSource.getTag();
		}

		@Override
		public void prepareSource(ExoPlayer player, boolean isTopLevelSource, SourceInfoRefreshListener listener) {
			this.mediaSource.prepareSource(player, isTopLevelSource, listener);
		}

		@Override
		public void prepareSource(ExoPlayer player, boolean isTopLevelSource, SourceInfoRefreshListener listener, @Nullable TransferListener mediaTransferListener) {
			mediaSource.prepareSource(player, isTopLevelSource, listener, mediaTransferListener);
		}

		@Override
		public void maybeThrowSourceInfoRefreshError() throws IOException {
			this.mediaSource.maybeThrowSourceInfoRefreshError();
		}

		@Override
		public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
			return this.mediaSource.createPeriod(id, allocator);
		}

		@Override
		public void releasePeriod(MediaPeriod mediaPeriod) {
			this.mediaSource.releasePeriod(mediaPeriod);
		}

		@Override
		public void releaseSource(SourceInfoRefreshListener listener) {
			this.mediaSource.releaseSource(listener);
			removeMediaSource(this);
		}
	}
}

