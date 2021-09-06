package com.lasthopesoftware.bluewater.tutorials

import android.content.Context
import com.lasthopesoftware.bluewater.repository.DatabasePromise
import com.lasthopesoftware.bluewater.repository.InsertBuilder
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tableName
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tutorialKeyColumn
import com.namehillsoftware.handoff.promises.Promise

class TutorialManager(private val context: Context) : ManageTutorials {
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

	override fun promiseIsTutorialShown(tutorialKey: String): Promise<Boolean> =
		promiseTutorial(tutorialKey).then { t -> t != null }

	override fun promiseTutorialMarked(tutorialKey: String): Promise<Unit> =
		promiseTutorial(tutorialKey).eventually { t ->
			if (t != null) Unit.toPromise() else DatabasePromise {
				RepositoryAccessHelper(context).use { h ->
					h.beginTransaction().use {
						h.mapSql(insertQuery)
							.addParameter(tutorialKeyColumn, tutorialKey)
							.execute()
					}
				}
			}.unitResponse()
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
