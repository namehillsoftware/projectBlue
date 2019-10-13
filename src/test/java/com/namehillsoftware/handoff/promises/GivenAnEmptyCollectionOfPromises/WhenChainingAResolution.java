package com.namehillsoftware.handoff.promises.GivenAnEmptyCollectionOfPromises;

import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.PromiseTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenChainingAResolution extends PromiseTestBase {
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
