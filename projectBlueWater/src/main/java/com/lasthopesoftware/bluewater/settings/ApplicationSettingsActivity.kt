package com.lasthopesoftware.bluewater.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineType
import com.lasthopesoftware.bluewater.client.playback.engine.selection.PlaybackEngineTypeSelectionPersistence
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.broadcast.PlaybackEngineTypeChangedBroadcaster
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
import com.lasthopesoftware.bluewater.client.playback.engine.selection.view.PlaybackEngineTypeSelectionView
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.servers.list.ServerListAdapter
import com.lasthopesoftware.bluewater.client.servers.list.listeners.EditServerClickListener
import com.lasthopesoftware.bluewater.client.settings.EditClientSettingsActivityIntentBuilder
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.intents.IntentFactory
import com.lasthopesoftware.resources.strings.StringResources

private val logger by lazyLogger<ApplicationSettingsActivity>()

class ApplicationSettingsActivity : AppCompatActivity() {
	private val progressBar by LazyViewFinder<ProgressBar>(this, R.id.items_loading_progress)
	private val serverListView by LazyViewFinder<RecyclerView>(this, R.id.loaded_recycler_view)
	private val addServerButton by LazyViewFinder<Button>(this, R.id.addServerButton)
	private val killPlaybackEngineButton by LazyViewFinder<Button>(this, R.id.killPlaybackEngine)
	private val settingsMenu by lazy { SettingsMenu(this, StringResources(this)) }
	private val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			this,
			EditClientSettingsActivityIntentBuilder(IntentFactory(this))
		)
	}
	private val applicationSettingsRepository by lazy { getApplicationSettingsRepository() }
	private val applicationMessageBus by lazyScoped { getApplicationMessageBus().getScopedMessageBus() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Ensure that this task is only started when it's the task root. A workaround for an Android bug.
		// See http://stackoverflow.com/a/7748416
		val intent = intent
		if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
			if (!isTaskRoot) {
				val className = javaClass.name
				logger.info("$className is not the root.  Finishing $className instead of launching.")
				finish()
				return
			}

			applicationSettingsRepository
				.promiseApplicationSettings()
				.then { s ->
					if (s.chosenLibraryId > -1)
						applicationNavigation.resetToBrowserRoot()
				}
		}

		setContentView(R.layout.activity_application_settings)
		setSupportActionBar(findViewById(R.id.applicationSettingsToolbar))

		val syncScheduler = SyncScheduler(this)
		HandleSyncCheckboxPreference.handle(
			applicationSettingsRepository,
			syncScheduler,
			{ s -> s.isSyncOnPowerOnly },
			{ s -> s::isSyncOnPowerOnly::set },
			findViewById(R.id.syncOnPowerCheckbox))

		HandleSyncCheckboxPreference.handle(
			applicationSettingsRepository,
			syncScheduler,
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
			PlaybackEngineTypeChangedBroadcaster(applicationMessageBus))

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

		killPlaybackEngineButton.setOnClickListener { PlaybackService.killService(this) }

		addServerButton.setOnClickListener(EditServerClickListener(this, -1))

		updateServerList()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = settingsMenu.buildSettingsMenu(menu)

	@Deprecated("Deprecated in Java")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		updateServerList()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = settingsMenu.handleSettingsMenuClicks(item)

	private fun updateServerList() {
		serverListView.visibility = View.INVISIBLE
		progressBar.visibility = View.VISIBLE

		val libraryProvider = LibraryRepository(this)
		val promisedLibraries = libraryProvider.allLibraries
		val promisedSelectedLibrary = getCachedSelectedLibraryIdProvider().promiseSelectedLibraryId()

		val adapter = ServerListAdapter(
			this,
			BrowserLibrarySelection(applicationSettingsRepository, applicationMessageBus, libraryProvider),
			applicationMessageBus)

		serverListView.adapter = adapter
		serverListView.layoutManager = LinearLayoutManager(this)

		promisedLibraries
			.eventually { libraries ->
				promisedSelectedLibrary
					.eventually(LoopedInPromise.response({ chosenLibraryId ->
						val selectedBrowserLibrary = libraries.firstOrNull { l -> l.libraryId == chosenLibraryId }

						adapter.updateLibraries(libraries, selectedBrowserLibrary)

						progressBar.visibility = View.INVISIBLE
						serverListView.visibility = View.VISIBLE
					}, this))
			}
	}

	companion object {
		fun launch(context: Context) =
			context.startActivity(Intent(context, ApplicationSettingsActivity::class.java))
	}
}
