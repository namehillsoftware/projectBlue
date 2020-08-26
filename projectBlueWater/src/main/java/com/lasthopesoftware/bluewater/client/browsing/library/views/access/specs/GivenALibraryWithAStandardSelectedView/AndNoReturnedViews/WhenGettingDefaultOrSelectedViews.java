package com.lasthopesoftware.bluewater.client.browsing.library.views.access.specs.GivenALibraryWithAStandardSelectedView.AndNoReturnedViews;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingDefaultOrSelectedViews {

	private static Item selectedLibraryView;
	private static Library savedLibrary;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SelectedLibraryViewProvider selectedLibraryViewProvider =
			new SelectedLibraryViewProvider(
				() -> new Promise<>(new Library().setSelectedView(5)),
				() -> new Promise<>(Collections.emptyList()),
				new ILibraryStorage() {
					@NotNull
					@Override
					public Promise<Library> saveLibrary(@NotNull Library library) {
						savedLibrary = library;
						return new Promise<>(library);
					}

					@NotNull
					@Override
					public Promise<Object> removeLibrary(@NotNull Library library) {
						return Promise.empty();
					}
				});
		selectedLibraryView = new FuturePromise<>(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get();
	}

	@Test
	public void thenNoSelectedViewIsReturned() {
		assertThat(selectedLibraryView).isNull();
	}

	@Test
	public void thenTheLibraryIsNotSaved() {
		assertThat(savedLibrary).isNull();
	}
}
