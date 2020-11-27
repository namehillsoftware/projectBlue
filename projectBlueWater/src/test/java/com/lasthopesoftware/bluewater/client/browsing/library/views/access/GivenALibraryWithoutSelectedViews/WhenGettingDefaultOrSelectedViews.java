package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithoutSelectedViews;

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingDefaultOrSelectedViews {

	private static final StandardViewItem expectedView = new StandardViewItem(2, null);
	private static ViewItem selectedLibraryView;
	private static Library savedLibrary;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SelectedLibraryViewProvider selectedLibraryViewProvider =
			new SelectedLibraryViewProvider(
				() -> new Promise<>(new Library()),
				() -> new Promise<>(
					Arrays.asList(
						new StandardViewItem(2, null),
						new StandardViewItem(1, null),
						new StandardViewItem(14, null))),
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
