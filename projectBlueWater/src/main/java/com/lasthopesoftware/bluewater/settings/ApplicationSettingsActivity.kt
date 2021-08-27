package com.lasthopesoftware.bluewater.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.about.AboutTitleBuilder
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
import com.lasthopesoftware.bluewater.client.playback.engine.selection.view.PlaybackEngineTypeSelectionView
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.servers.list.ServerListAdapter
import com.lasthopesoftware.bluewater.client.servers.list.listeners.EditServerClickListener
import com.lasthopesoftware.bluewater.settings.repository.access.ApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.SharedChannelProperties
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import tourguide.tourguide.Overlay
import tourguide.tourguide.Pointer
import tourguide.tourguide.ToolTip
import tourguide.tourguide.TourGuide

class ApplicationSettingsActivity : AppCompatActivity() {
	private val channelConfiguration: SharedChannelProperties by lazy { SharedChannelProperties(this) }
	private val progressBar = LazyViewFinder<ProgressBar>(this, R.id.recyclerLoadingProgress)
	private val serverListView = LazyViewFinder<RecyclerView>(this, R.id.loadedRecyclerView)
	private val notificationSettingsContainer = LazyViewFinder<LinearLayout>(this, R.id.notificationSettingsContainer)
	private val modifyNotificationSettingsButton = LazyViewFinder<Button>(this, R.id.modifyNotificationSettingsButton)
	private val addServerButton = LazyViewFinder<Button>(this, R.id.addServerButton)
	private val killPlaybackEngineButton = LazyViewFinder<Button>(this, R.id.killPlaybackEngine)
	private val settingsMenu = SettingsMenu(this, AboutTitleBuilder(this))
	private val lazySharedPreferences = lazy { PreferenceManager.getDefaultSharedPreferences(this) }
	private val applicationSettingsRepository: ApplicationSettingsRepository by lazy { ApplicationSettingsRepository(this) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_application_settings)
		setSupportActionBar(findViewById(R.id.applicationSettingsToolbar))

		val sharedPreferences = lazySharedPreferences.value
		HandleSyncCheckboxPreference.handle(
			applicationSettingsRepository,
			{ s -> s.isSyncOnPowerOnly },
			{ s -> s::isSyncOnPowerOnly::set },
			findViewById(R.id.syncOnPowerCheckbox))

		HandleSyncCheckboxPreference.handle(
			applicationSettingsRepository,
			{ it.isSyncOnWifiOnly },
			{ s -> s::isSyncOnWifiOnly::set },
			findViewById(R.id.syncOnWifiCheckbox))

		HandleCheckboxPreference.handle(
			applicationSettingsRepository,
			{ it.isVolumeLevelingEnabled },
			{ s -> s::isVolumeLevelingEnabled::set },
			findViewById(R.id.isVolumeLevelingEnabled))

		val selection = PlaybackEngineTypeSelectionPersistence(
			applicationSettingsRepository,
			PlaybackEngineTypeChangedBroadcaster(MessageBus(LocalBroadcastManager.getInstance(this))))

		val selectedPlaybackEngineTypeAccess = SelectedPlaybackEngineTypeAccess(applicationSettingsRepository, DefaultPlaybackEngineLookup())

		val playbackEngineTypeSelectionView = PlaybackEngineTypeSelectionView(this)

		val playbackEngineOptions = findViewById<RadioGroup>(R.id.playbackEngineOptions)

		for (i in 0 until playbackEngineOptions.childCount)
			playbackEngineOptions.getChildAt(i).isEnabled = false

		for (rb in playbackEngineTypeSelectionView.buildPlaybackEngineTypeSelections())
			playbackEngineOptions.addView(rb)

		selectedPlaybackEngineTypeAccess.promiseSelectedPlaybackEngineType()
			.eventually(LoopedInPromise.response({ t ->
				playbackEngineOptions.check(t.ordinal)
				for (i in 0 until playbackEngineOptions.childCount)
					playbackEngineOptions.getChildAt(i).isEnabled = true
			}, this))

		playbackEngineOptions
			.setOnCheckedChangeListener { _, checkedId -> selection.selectPlaybackEngine(PlaybackEngineType.values()[checkedId]) }

		killPlaybackEngineButton.findView().setOnClickListener { PlaybackService.killService(this) }

		addServerButton.findView().setOnClickListener(EditServerClickListener(this, -1))

		updateServerList()

		notificationSettingsContainer.findView().visibility = View.GONE
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

		notificationSettingsContainer.findView().visibility = View.VISIBLE

		val wasTutorialShown = sharedPreferences.getBoolean(isTutorialShownPreference, false)
		if (wasTutorialShown) {
			modifyNotificationSettingsButton.findView().setOnClickListener { launchNotificationSettings() }
			return
		}

		val displayColor = getColor(R.color.clearstream_blue)
		val tourGuide = TourGuide.init(this).with(TourGuide.Technique.CLICK)
			.setPointer(Pointer().setColor(displayColor))
			.setToolTip(ToolTip()
				.setTitle(getString(R.string.notification_settings_tutorial_title))
				.setDescription(getString(R.string.notification_settings_tutorial).format(
					getString(R.string.modify_notification_settings),
					getString(R.string.app_name)))
				.setBackgroundColor(displayColor))
			.setOverlay(Overlay())
			.playOn(modifyNotificationSettingsButton.findView())

		modifyNotificationSettingsButton.findView().setOnClickListener {
			tourGuide.cleanUp()
			launchNotificationSettings()
		}

		sharedPreferences.edit().putBoolean(isTutorialShownPreference, true).apply()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = settingsMenu.buildSettingsMenu(menu)

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		updateServerList()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = settingsMenu.handleSettingsMenuClicks(item)

	@RequiresApi(api = Build.VERSION_CODES.O)
	private fun launchNotificationSettings() {
		val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
		intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
		intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelConfiguration.channelId)
		startActivity(intent)
	}

	private fun updateServerList() {
		serverListView.findView().visibility = View.INVISIBLE
		progressBar.findView().visibility = View.VISIBLE

		val libraryProvider = LibraryRepository(this)
		val promisedLibraries = libraryProvider.allLibraries

		val adapter = ServerListAdapter(
			this,
			BrowserLibrarySelection(this, LocalBroadcastManager.getInstance(this), libraryProvider))

		val serverListView = serverListView.findView()
		serverListView.adapter = adapter
		serverListView.layoutManager = LinearLayoutManager(this)

		promisedLibraries
			.eventually(LoopedInPromise.response({ libraries ->
				val chosenLibraryId = SelectedBrowserLibraryIdentifierProvider(this).selectedLibraryId
				val selectedBrowserLibrary = libraries.firstOrNull { l -> l.libraryId == chosenLibraryId }

				adapter.updateLibraries(libraries, selectedBrowserLibrary)

				progressBar.findView().visibility = View.INVISIBLE
				serverListView.visibility = View.VISIBLE
			}, this))
	}

	companion object {
		fun launch(context: Context) =
			context.startActivity(Intent(context, ApplicationSettingsActivity::class.java))

		private const val isTutorialShownPreference = "isApplicationSettingsTutorialShownPreference"
	}
}
