package com.lasthopesoftware.bluewater.client.playback.playlist.specs.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback;

import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile;
import com.lasthopesoftware.bluewater.client.playback.file.specs.fakes.FakeBufferingPlaybackHandler;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.observables.ConnectableObservable;

public class ResolveablePlaybackHandler extends FakeBufferingPlaybackHandler {

	private final ConnectableObservable<Duration> observable;
	private ObservableEmitter<Duration> observableEmitter;

	public ResolveablePlaybackHandler() {
		observable = Observable.<Duration>create(e -> {
			observableEmitter = e;
		}).publish();

		observable.connect();
	}

	public void resolve() {
		if (observableEmitter != null)
			observableEmitter.onComplete();

		observableEmitter = null;
	}

	@Override
	public Observable<Duration> observeProgress(Duration observationPeriod) {
		return super.observeProgress(observationPeriod)
			.concatWith(observable);
	}

	@Override
	public Promise<PlayingFile> promisePlayback() {
		super.promisePlayback();
		return new Promise<>(this);
	}
}
