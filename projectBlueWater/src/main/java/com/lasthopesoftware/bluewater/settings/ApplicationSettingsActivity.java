package com.lasthopesoftware.bluewater.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.servers.list.ServerListAdapter;
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;

import static com.lasthopesoftware.messenger.promises.response.ImmediateAction.perform;

public class ApplicationSettingsActivity extends AppCompatActivity {
	private LazyViewFinder<ProgressBar> progressBar = new LazyViewFinder<>(this, R.id.pbLoadingServerList);
	private LazyViewFinder<ListView> serverListView = new LazyViewFinder<>(this, R.id.lvServerList);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_server);

		final RelativeLayout editAppSettingsView = (RelativeLayout) getLayoutInflater().inflate(R.layout.layout_edit_app_settings, serverListView.findView(), false);
		serverListView.findView().addHeaderView(editAppSettingsView);

		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isSyncOnPowerOnlyKey, (CheckBox) findViewById(R.id.syncOnPowerCheckbox));
		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isSyncOnWifiOnlyKey, (CheckBox) findViewById(R.id.syncOnWifiCheckbox));
		HandleCheckboxPreference.handle(this, ApplicationConstants.PreferenceConstants.isVolumeLevelingEnabled, (CheckBox) findViewById(R.id.isVolumeLevelingEnabled));

		updateServerList();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		updateServerList();
	}

	private void updateServerList() {
		final Activity activity = this;

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
						activity,
						Stream.of(libraries).sortBy(Library::getId).collect(Collectors.toList()),
						selectedBrowserLibrary.isPresent() ? selectedBrowserLibrary.get() : null,
						new BrowserLibrarySelection(this, LocalBroadcastManager.getInstance(this), libraryProvider)));

				progressBar.findView().setVisibility(View.INVISIBLE);
				serverListView.findView().setVisibility(View.VISIBLE);
			}), this));
	}
}
