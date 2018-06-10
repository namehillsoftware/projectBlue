package com.lasthopesoftware.bluewater.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.ApplicationConstants;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.about.AboutTitleBuilder;
import com.lasthopesoftware.bluewater.client.library.access.ILibraryProvider;
import com.lasthopesoftware.bluewater.client.library.access.LibraryRepository;
import com.lasthopesoftware.bluewater.client.library.items.list.DemoableItemListAdapter;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup;
import com.lasthopesoftware.bluewater.client.playback.engine.selection.view.PlaybackEngineTypeSelectionView;
import com.lasthopesoftware.bluewater.client.servers.list.ServerListAdapter;
import com.lasthopesoftware.bluewater.client.servers.selection.BrowserLibrarySelection;
import com.lasthopesoftware.bluewater.client.servers.selection.SelectedBrowserLibraryIdentifierProvider;
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder;
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder;
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise;
import com.lasthopesoftware.resources.notifications.notificationchannel.ChannelConfiguration;
import com.lasthopesoftware.resources.notifications.notificationchannel.SharedChannelProperties;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

import static com.namehillsoftware.handoff.promises.response.ImmediateAction.perform;

public class ApplicationSettingsActivity extends AppCompatActivity {
	private static final String isTutorialShownPreference = MagicPropertyBuilder.buildMagicPropertyName(DemoableItemListAdapter.class, "TUTORIAL_SHOWN");

	private final CreateAndHold<ChannelConfiguration> lazyChannelConfiguration = new Lazy<>(() -> new SharedChannelProperties(this));
	private final LazyViewFinder<ProgressBar> progressBar = new LazyViewFinder<>(this, R.id.listLoadingProgress);
	private final LazyViewFinder<ListView> serverListView = new LazyViewFinder<>(this, R.id.loadedListView);
	private final LazyViewFinder<LinearLayout> notificationSettingsContainer = new LazyViewFinder<>(this, R.id.notificationSettingsContainer);
	private final LazyViewFinder<Button> modifyNotificationSettingsButton = new LazyViewFinder<>(this, R.id.modifyNotificationSettingsButton);
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

		final PlaybackEngineTypeSelectionPersistence selection = new PlaybackEngineTypeSelectionPersistence(
			this,
			new PlaybackEngineTypeChangedBroadcaster(this));

		final SelectedPlaybackEngineTypeAccess selectedPlaybackEngineTypeAccess =
			new SelectedPlaybackEngineTypeAccess(this, new DefaultPlaybackEngineLookup());

		final PlaybackEngineTypeSelectionView playbackEngineTypeSelectionView =
			new PlaybackEngineTypeSelectionView(this);

		final RadioGroup playbackEngineOptions = findViewById(R.id.playbackEngineOptions);

		for(int i = 0; i < playbackEngineOptions.getChildCount(); i++)
			playbackEngineOptions.getChildAt(i).setEnabled(false);

		playbackEngineTypeSelectionView.buildPlaybackEngineTypeSelections()
			.forEach(playbackEngineOptions::addView);

		selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType()
			.eventually(LoopedInPromise.response(t -> {
				playbackEngineOptions.check(t.ordinal());

				for(int i = 0; i < playbackEngineOptions.getChildCount(); i++)
					playbackEngineOptions.getChildAt(i).setEnabled(true);

				return null;
			}, this));

		playbackEngineOptions
			.setOnCheckedChangeListener((group, checkedId) -> selection.selectPlaybackEngine(PlaybackEngineType.values()[checkedId]));

		updateServerList();

		notificationSettingsContainer.findView().setVisibility(View.GONE);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

		notificationSettingsContainer.findView().setVisibility(View.VISIBLE);

		final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean wasTutorialShown = sharedPreferences.getBoolean(isTutorialShownPreference, false);
		if (wasTutorialShown) {
			modifyNotificationSettingsButton.findView().setOnClickListener(v -> launchNotificationSettings());
			return;
		}

		final int displayColor = getColor(R.color.clearstream_blue);

		final TourGuide tourGuide =
			TourGuide.init(this).with(TourGuide.Technique.CLICK)
				.setPointer(new Pointer().setColor(displayColor))
				.setToolTip(new ToolTip()
					.setTitle(getString(R.string.notification_settings_tutorial_title))
					.setDescription(String.format(getString(R.string.notification_settings_tutorial), getString(R.string.app_name)))
					.setBackgroundColor(displayColor))
				.setOverlay(new Overlay())
				.playOn(modifyNotificationSettingsButton.findView())
				.motionType(TourGuide.MotionType.CLICK_ONLY);

		modifyNotificationSettingsButton.findView().setOnClickListener(v -> {
			tourGuide.cleanUp();

			launchNotificationSettings();
		});
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

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void launchNotificationSettings() {
		final Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
		intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
		intent.putExtra(Settings.EXTRA_CHANNEL_ID, lazyChannelConfiguration.getObject().getChannelId());
		startActivity(intent);
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
