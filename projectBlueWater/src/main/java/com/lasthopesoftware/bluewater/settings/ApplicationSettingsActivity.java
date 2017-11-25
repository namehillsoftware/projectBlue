package com.lasthopesoftware.bluewater.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.about.AboutTitleBuilder;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.list.ServerListAdapter;
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class ApplicationSettingsActivity extends AppCompatActivity {
	private final LazyViewFinder<ProgressBar> progressBar = new LazyViewFinder<>(this, R.id.listLoadingProgress);
	private final LazyViewFinder<ListView> serverListView = new LazyViewFinder<>(this, R.id.loadedListView);
	private final SettingsMenu settingsMenu = new SettingsMenu(this, new AboutTitleBuilder(this));

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.asynchronous_list_view);

		final RelativeLayout editAppSettingsView = (RelativeLayout) getLayoutInflater().inflate(R.layout.layout_edit_app_settings, serverListView.findView(), false);
		serverListView.findView().addHeaderView(editAppSettingsView);

		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, findViewById(R.id.syncOnPowerCheckbox));
		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, findViewById(R.id.syncOnWifiCheckbox));
		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, findViewById(R.id.isVolumeLevelingEnabled));

		updateServerList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return settingsMenu.buildSettingsMenu(menu);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		updateServerList();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return settingsMenu.handleSettingsMenuClicks(item);
	}

	private void updateServerList() {
		serverListView.findView().setVisibility(View.INVISIBLE);
		progressBar.findView().setVisibility(View.VISIBLE);

		final ILibraryProvider libraryProvider = new LibraryRepository(this);

		libraryProvider
			.getAllLibraries()
			.eventually(LoopedInPromise.response(perform(libraries -> {
				final int chosenLibraryId = new SelectedBrowserLibraryIdentifierProvider(this).getSelectedLibraryId();

				final Optional<Library> selectedBrowserLibrary = Stream.of(libraries).filter(l -> l.getId() == chosenLibraryId).findFirst();

				serverListView.findView().setAdapter(
					new ServerListAdapter(
						this,
						Stream.of(libraries).sortBy(Library::getId).collect(Collectors.toList()),
						selectedBrowserLibrary.isPresent() ? selectedBrowserLibrary.get() : null,
						new BrowserLibrarySelection(this, LocalBroadcastManager.getInstance(this), libraryProvider)));

				progressBar.findView().setVisibility(View.INVISIBLE);
				serverListView.findView().setVisibility(View.VISIBLE);
			}), this));
	}
}
