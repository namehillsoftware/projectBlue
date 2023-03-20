package com.lasthopesoftware.bluewater.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
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
import com.lasthopesoftware.bluewater.databinding.ActivityApplicationSettingsBinding
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.android.view.getValue
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.suspend
import com.lasthopesoftware.resources.closables.lazyScoped
import com.lasthopesoftware.resources.intents.IntentFactory
import com.lasthopesoftware.resources.strings.StringResources
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private val logger by lazyLogger<ApplicationSettingsActivity>()

class ApplicationSettingsActivity : AppCompatActivity() {
	private val serverListView by LazyViewFinder<RecyclerView>(this, R.id.loaded_recycler_view)
	private val settingsMenu by lazy { SettingsMenu(this, StringResources(this)) }
	private val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			this,
			EditClientSettingsActivityIntentBuilder(IntentFactory(this))
		)
	}
	private val applicationSettingsRepository by lazy { getApplicationSettingsRepository() }
	private val selectedPlaybackEngineTypeAccess by lazy {
		SelectedPlaybackEngineTypeAccess(
			applicationSettingsRepository,
			DefaultPlaybackEngineLookup()
		)
	}
	private val libraryProvider by lazy { LibraryRepository(this) }
	private val applicationMessageBus by lazyScoped { getApplicationMessageBus().getScopedMessageBus() }
	private val viewModel by buildViewModelLazily {
		ApplicationSettingsViewModel(
			applicationSettingsRepository,
			selectedPlaybackEngineTypeAccess,
			libraryProvider,
		)
	}

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

		setSupportActionBar(findViewById(R.id.applicationSettingsToolbar))

		val binding = DataBindingUtil.setContentView<ActivityApplicationSettingsBinding>(this, R.layout.activity_application_settings)
		binding.vm = viewModel

		val syncScheduler = SyncScheduler(this)
		viewModel.isVolumeLevelingEnabled.drop(1).onEach {
			viewModel.saveSettings().suspend()
		}.launchIn(lifecycleScope)

		viewModel.isSyncOnPowerOnly.drop(1).onEach {
			viewModel.saveSettings().suspend()
			syncScheduler.scheduleSync().suspend()
		}.launchIn(lifecycleScope)

		viewModel.isSyncOnWifiOnly.drop(1).onEach {
			viewModel.saveSettings().suspend()
			syncScheduler.scheduleSync().suspend()
		}.launchIn(lifecycleScope)

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

		binding.killPlaybackEngine.setOnClickListener { PlaybackService.killService(this) }

		binding.addServerButton.setOnClickListener(EditServerClickListener(this, -1))

		val adapter = ServerListAdapter(
			this,
			BrowserLibrarySelection(applicationSettingsRepository, applicationMessageBus, libraryProvider),
			applicationMessageBus)

		serverListView.adapter = adapter
		serverListView.layoutManager = LinearLayoutManager(this)

		viewModel.libraries.onEach {
			val selectedBrowserLibrary = it.firstOrNull { l -> l.libraryId == viewModel.chosenLibraryId.value }

			adapter.updateLibraries(it, selectedBrowserLibrary).suspend()
		}.launchIn(lifecycleScope)

		viewModel.loadSettings()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean = settingsMenu.buildSettingsMenu(menu)

	@Deprecated("Deprecated in Java")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		viewModel.loadSettings()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = settingsMenu.handleSettingsMenuClicks(item)

	companion object {
		fun launch(context: Context) =
			context.startActivity(Intent(context, ApplicationSettingsActivity::class.java))
	}
}
