package com.lasthopesoftware.bluewater.client.browsing

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.lasthopesoftware.bluewater.ApplicationDependencies
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryViewModel
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.connection.trust.ProvideUserSslCertificates
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsViewModel
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages

interface EntryDependencies : ApplicationDependencies {
	val selectedLibraryViewModel: SelectedLibraryViewModel
	val libraryRemoval: RemoveLibraries
	val menuMessageBus: ViewModelMessageBus<ItemListMenuMessage>
	val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
	val applicationNavigation: NavigateApplication
	val storedFileAccess: StoredFileAccess
	val navigationMessages: RegisterForTypedMessages<NavigationMessage>
	val applicationSettingsRepository: HoldApplicationSettings
	val reusableChildItemViewModelProvider: ReusableChildItemViewModelProvider
	val applicationSettingsViewModel: ApplicationSettingsViewModel
	val hiddenSettingsViewModel: HiddenSettingsViewModel
	val userSslCertificateProvider: ProvideUserSslCertificates
}

@Composable
fun <T: EntryDependencies> T.registerBackNav() : T {
	BackHandler {
		applicationNavigation.backOut()
	}

	return this
}
