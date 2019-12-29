package com.lasthopesoftware.bluewater.client.library.views.access.specs.GivenALibraryWithoutSelectedViews;

import com.lasthopesoftware.bluewater.client.library.items.Item;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingDefaultOrSelectedViews {

	private static Item expectedView = new Item(2);
	private static Item selectedLibraryView;
	private static Library savedLibrary;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SelectedLibraryViewProvider selectedLibraryViewProvider =
			new SelectedLibraryViewProvider(
				() -> new Promise<>(new Library()),
				() -> new Promise<>(
					Arrays.asList(
						new Item(2),
						new Item(1),
						new Item(14))),
				library -> {
					savedLibrary = library;
					return new Promise<>(library);
				});
		selectedLibraryView = new FuturePromise<>(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get();
	}

	@Test
	public void thenTheSelectedViewsAreCorrect() {
		assertThat(selectedLibraryView).isEqualTo(expectedView);
	}

	@Test
	public void thenTheSelectedViewKeyIsSaved() {
		assertThat(savedLibrary.getSelectedView()).isEqualTo(expectedView.getKey());
	}

	@Test
	public void thenTheSelectedViewTypeIsStandard() {
		assertThat(savedLibrary.getSelectedViewType()).isEqualTo(Library.ViewType.StandardServerView);
	}
}
