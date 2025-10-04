package com.lasthopesoftware.bluewater

import androidx.activity.ComponentActivity
import com.lasthopesoftware.bluewater.client.ActivitySuppliedDependencies
import com.lasthopesoftware.bluewater.client.browsing.EntryDependencies
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryViewModel
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.connection.trust.UserSslCertificateProvider
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsViewModel
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.resources.closables.ViewModelCloseableManager
import com.lasthopesoftware.resources.uri.DocumentUriSelector

class ActivityDependencies(
	activity: ComponentActivity,
	activitySuppliedDependencies: ActivitySuppliedDependencies,
	applicationDependencies: ApplicationDependencies
) : EntryDependencies, ApplicationDependencies by applicationDependencies {
	private val applicationContext by lazy { activity.applicationContext }

	private val viewModelScope by activity.buildViewModelLazily { ViewModelCloseableManager() }

	private val selectedPlaybackEngineTypeAccess by lazy {
		SelectedPlaybackEngineTypeAccess(
			applicationSettings,
			DefaultPlaybackEngineLookup
		)
	}

	private val messageBus by lazy { ApplicationMessageBus.getApplicationMessageBus().getScopedMessageBus().also(viewModelScope::manage) }

	private val libraryBrowserSelection by lazy {
		BrowserLibrarySelection(
			applicationSettings,
			messageBus,
			libraryProvider,
		)
	}

	override val registerForApplicationMessages: RegisterForApplicationMessages
		get() = messageBus

	override val sendApplicationMessages: SendApplicationMessages
		get() = messageBus

	override val menuMessageBus by activity.buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	override val itemListMenuBackPressedHandler by lazy { ItemListMenuBackPressedHandler(menuMessageBus).also(viewModelScope::manage) }

	override val storedItemAccess by lazy {
		StateChangeBroadcastingStoredItemAccess(applicationDependencies.storedItemAccess, messageBus)
	}

	override val storedFileAccess by lazy { StoredFileAccess(applicationContext) }

	override val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			activity,
			intentBuilder,
		)
	}

	override val librarySettingsStorage by lazy {
		ObservableConnectionSettingsLibraryStorage(
			applicationDependencies.librarySettingsStorage,
			connectionSettingsLookup,
			sendApplicationMessages
		)
	}

	override val libraryRemoval by lazy {
		LibraryRemoval(
			storedItemAccess,
			libraryStorage,
			selectedLibraryIdProvider,
			libraryProvider,
			libraryBrowserSelection,
		)
	}

	override val navigationMessages by activity.buildViewModelLazily { ViewModelMessageBus<NavigationMessage>() }

	override val applicationViewModel by activity.buildViewModelLazily {
		ApplicationViewModel(
			applicationSettings,
			messageBus,
		)
	}

	override val selectedLibraryViewModel: SelectedLibraryViewModel by activity.buildViewModelLazily {
		SelectedLibraryViewModel(
			selectedLibraryIdProvider,
			libraryBrowserSelection,
		).apply { loadSelectedLibraryId() }
	}

	override val reusableChildItemViewModelProvider by activity.buildViewModelLazily {
		ReusableChildItemViewModelProvider(
			storedItemAccess,
			menuMessageBus,
		)
	}

	override val applicationSettingsViewModel by activity.buildViewModelLazily {
		ApplicationSettingsViewModel(
			applicationSettings,
			selectedPlaybackEngineTypeAccess,
			librarySettingsProvider,
			libraryNameLookup,
			messageBus,
			syncScheduler,
		)
	}

	override val hiddenSettingsViewModel by activity.buildViewModelLazily {
		HiddenSettingsViewModel(applicationSettings)
	}

	override val userSslCertificateProvider by lazy {
		UserSslCertificateProvider(
			DocumentUriSelector(activitySuppliedDependencies.registeredActivityResultsLauncher),
			activity.contentResolver
		)
	}
}
