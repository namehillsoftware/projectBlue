package com.lasthopesoftware.bluewater.tutorials

import android.content.Context
import com.lasthopesoftware.bluewater.repository.DatabasePromise
import com.lasthopesoftware.bluewater.repository.InsertBuilder
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tableName
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tutorialKeyColumn
import com.namehillsoftware.handoff.promises.Promise

class TutorialManager(private val context: Context, private val tutorialCache: CacheTutorialInformation = TutorialInformationCache) : ManageTutorials {
	object KnownTutorials {
		const val longPressListTutorial = "longPressListTutorial"
		const val adjustNotificationInApplicationSettingsTutorial = "adjustNotificationInApplicationSettings"
	}

	companion object {
		private val insertQuery by lazy {
			InsertBuilder.fromTable(tableName)
				.addColumn(tutorialKeyColumn)
				.build()
		}
	}

	override fun promiseWasTutorialShown(tutorialKey: String): Promise<Boolean> =
		tutorialCache[tutorialKey]?.toPromise()
			?: promiseTutorial(tutorialKey).then { t ->
				val wasShown = t != null
				tutorialCache[tutorialKey] = wasShown
				wasShown
			}

	override fun promiseTutorialMarked(tutorialKey: String): Promise<Unit> =
		promiseWasTutorialShown(tutorialKey).eventually { wasShown ->
			if (wasShown) Unit.toPromise()
			else DatabasePromise {
				RepositoryAccessHelper(context).use { h ->
					h.beginTransaction().use {
						h.mapSql(insertQuery)
							.addParameter(tutorialKeyColumn, tutorialKey)
							.execute()
						it.setTransactionSuccessful()
					}
				}
			}.then {
				tutorialCache[tutorialKey] = true
			}
		}

	private fun promiseTutorial(tutorialKey: String): Promise<DisplayedTutorial?> =
		DatabasePromise {
			RepositoryAccessHelper(context).use { h ->
				h.beginNonExclusiveTransaction().use {
					h.mapSql("SELECT * FROM $tableName WHERE $tutorialKeyColumn = @$tutorialKeyColumn")
						.addParameter(tutorialKeyColumn, tutorialKey)
						.fetchFirst()
				}
			}
		}
}
