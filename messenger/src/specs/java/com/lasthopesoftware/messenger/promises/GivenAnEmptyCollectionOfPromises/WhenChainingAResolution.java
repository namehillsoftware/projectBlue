package com.lasthopesoftware.messenger.promises.GivenAnEmptyCollectionOfPromises;

import com.lasthopesoftware.messenger.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenChainingAResolution {
	private static ArrayList<String> result;

	@BeforeClass
	public static void before() {
		Promise.<String>whenAll()
			.then(strings -> result = new ArrayList<>(strings));
	}

	@Test
	public void thenTheResolutionIsCorrect() {
		assertThat(result).isEmpty();
	}
}
