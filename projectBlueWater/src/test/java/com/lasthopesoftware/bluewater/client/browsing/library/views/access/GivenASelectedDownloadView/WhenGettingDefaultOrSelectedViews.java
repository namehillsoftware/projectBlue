package com.lasthopesoftware.bluewater.client.browsing.library.views.access.GivenASelectedDownloadView;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.browsing.library.views.DownloadViewItem;
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem;
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

	private static final DownloadViewItem expectedView = new DownloadViewItem();
	private static final SavedLibraryRecordingStorage libraryStorage = new SavedLibraryRecordingStorage();
	private static ViewItem selectedLibraryView;

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final SelectedLibraryViewProvider selectedLibraryViewProvider =
			new SelectedLibraryViewProvider(
				() -> new Promise<>(new Library().setSelectedView(8).setSelectedViewType(Library.ViewType.DownloadView)),
				() -> new Promise<>(
					Arrays.asList(
						new StandardViewItem(3, null),
						new StandardViewItem(5, null),
						new PlaylistViewItem(8))),
				libraryStorage);
		selectedLibraryView = new FuturePromise<>(selectedLibraryViewProvider.promiseSelectedOrDefaultView()).get();
	}

	@Test
	public void thenTheSelectedViewsAreCorrect() {
		assertThat(selectedLibraryView).isEqualTo(expectedView);
	}

	@Test
	public void thenTheLibraryIsNotSaved() {
		assertThat(libraryStorage.getSavedLibrary()).isNull();
	}
}
