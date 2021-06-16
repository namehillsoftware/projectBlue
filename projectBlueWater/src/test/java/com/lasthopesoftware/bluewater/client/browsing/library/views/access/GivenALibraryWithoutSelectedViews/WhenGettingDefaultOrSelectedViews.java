package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenALibraryWithoutSelectedViews;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.ViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SavedLibraryRecordingStorage;
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingDefaultOrSelectedViews {

	private static final StandardViewItem expectedView = new StandardViewItem(2, null);
	private static final SavedLibraryRecordingStorage libraryStorage = new SavedLibraryRecordingStorage();
	private static ViewItem selectedLibraryView;

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
				libraryStorage);
		selectedLibraryView = new FuturePromise<>(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get();
	}

	@Test
	public void thenTheSelectedViewsAreCorrect() {
		assertThat(selectedLibraryView).isEqualTo(expectedView);
	}

	@Test
	public void thenTheSelectedViewKeyIsSaved() {
		assertThat(libraryStorage.getSavedLibrary().getSelectedView()).isEqualTo(expectedView.getKey());
	}

	@Test
	public void thenTheSelectedViewTypeIsStandard() {
		assertThat(libraryStorage.getSavedLibrary().getSelectedViewType()).isEqualTo(Library.ViewType.StandardServerView);
	}
}
