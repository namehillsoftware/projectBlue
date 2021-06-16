package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithAStandardSelectedView.AndNoReturnedViews;

import com.lasthopesoftware.bluewater.client.browsing.items.Item;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SavedLibraryRecordingStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingDefaultOrSelectedViews {

	private static final SavedLibraryRecordingStorage libraryStorage = new SavedLibraryRecordingStorage();
	private static Item selectedLibraryView;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SelectedLibraryViewProvider selectedLibraryViewProvider =
			new SelectedLibraryViewProvider(
				() -> new Promise<>(new Library().setSelectedView(5)),
				() -> new Promise<>(Collections.emptyList()),
				libraryStorage);
		selectedLibraryView = new FuturePromise<>(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get();
	}

	@Test
	public void thenNoSelectedViewIsReturned() {
		assertThat(selectedLibraryView).isNull();
	}

	@Test
	public void thenTheLibraryIsNotSaved() {
		assertThat(libraryStorage.getSavedLibrary()).isNull();
	}
}
