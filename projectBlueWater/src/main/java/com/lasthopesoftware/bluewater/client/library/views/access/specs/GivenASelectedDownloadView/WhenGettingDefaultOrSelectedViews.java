package com.lasthopesoftware.bluewater.client.library.views.access.specs.GivenASelectedDownloadView;

import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.views.DownloadViewItem;
import com.lasthopesoftware.bluewater.client.library.views.PlaylistViewItem;
import com.lasthopesoftware.bluewater.client.library.views.StandardViewItem;
import com.lasthopesoftware.bluewater.client.library.views.ViewItem;
import com.lasthopesoftware.bluewater.client.library.views.access.SelectedLibraryViewProvider;
import com.lasthopesoftware.bluewater.shared.promises.extensions.specs.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenGettingDefaultOrSelectedViews {

	private static DownloadViewItem expectedView = new DownloadViewItem();
	private static ViewItem selectedLibraryView;
	private static Library savedLibrary;

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
	public void thenTheLibraryIsNotSaved() {
		assertThat(savedLibrary).isNull();
	}
}
