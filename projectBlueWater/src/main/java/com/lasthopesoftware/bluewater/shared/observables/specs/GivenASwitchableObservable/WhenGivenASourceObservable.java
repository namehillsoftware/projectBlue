package com.lasthopesoftware.bluewater.shared.observables.specs.GivenASwitchableObservable;


import com.lasthopesoftware.bluewater.shared.observables.SwitchableObservableSource;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import io.reactivex.Observable;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGivenASourceObservable {

	private static List<Integer> replayedSource;

	@BeforeClass
	public static void before() {
		replayedSource =
			Observable
				.create(new SwitchableObservableSource<Integer>(Observable.just(1, 2, 3)))
				.toList()
				.blockingGet();
	}

	@Test
	public void thenTheSourceIsReplayed() {
		assertThat(replayedSource).containsExactly(1, 2, 3);
	}
}
