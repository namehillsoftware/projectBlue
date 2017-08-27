package com.lasthopesoftware.bluewater.shared.observables.specs.GivenASwitchableObservable;


import com.lasthopesoftware.bluewater.shared.observables.SwitchableObservableSource;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGivenASourceObservableAndTheSourceIsSwitchedHalfwayThrough {

	private static final List<Integer> replayedSource = new ArrayList<>();

	@BeforeClass
	public static void before() {
		final Emitter emitter = new Emitter();

		final SwitchableObservableSource<Integer> switchableObservableSource =
			new SwitchableObservableSource<>(Observable.create(emitter));

		Observable
			.create(switchableObservableSource)
			.subscribe(replayedSource::add);

		emitter.emit(1);
		emitter.emit(2);

		switchableObservableSource.switchSource(Observable.just(7, 4, 8));

		emitter.emit(3);
	}

	@Test
	public void thenBothSourcesAreEmittedCorrectly() {
		assertThat(replayedSource).containsExactly(1, 2, 7, 4, 8);
	}

	private static class Emitter implements ObservableOnSubscribe<Integer> {
		private ObservableEmitter<Integer> emitter;

		void emit(int i) {
			if (emitter != null)
				emitter.onNext(i);
		}

		@Override
		public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
			emitter = e;
		}
	}
}
