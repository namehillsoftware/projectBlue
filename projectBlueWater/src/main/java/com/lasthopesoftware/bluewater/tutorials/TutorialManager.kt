package com.lasthopesoftware.bluewater.tutorials

import android.content.Context
import com.lasthopesoftware.bluewater.repository.*
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.bluewater.tutorials.TutorialEntityInformation.isShownColumn
import com.lasthopesoftware.bluewater.tutorials.TutorialEntityInformation.tutorialKeyColumn
import com.namehillsoftware.handoff.promises.Promise

class TutorialManager(private val context: Context) : ManageTutorials {
	companion object KnownTutorials {
		const val longPressListTutorial = "longPressListTutorial"
		const val adjustNotificationInApplicationSettingsTutorial = "adjustNotificationInApplicationSettings"

		private val insertQuery by lazy {
			InsertBuilder.fromTable(TutorialEntityInformation.tableName)
				.addColumn(tutorialKeyColumn)
				.addColumn(isShownColumn)
				.build()
		}

		private val updateQuery by lazy {
			UpdateBuilder.fromTable(TutorialEntityInformation.tableName)
				.addSetter(isShownColumn)
				.setFilter(" WHERE $tutorialKeyColumn = @tutorialKeyColumn ")
				.buildQuery()
		}
	}

	override fun promiseIsTutorialShown(tutorialKey: String): Promise<Boolean> =
		promiseTutorial(tutorialKey).then { t -> t?.isShown ?: false }

	override fun promiseTutorialMarked(tutorialKey: String): Promise<Unit> =
		promiseTutorial(tutorialKey).eventually { t ->
			if (t?.isShown == true) Unit.toPromise() else DatabasePromise {
				RepositoryAccessHelper(context).use { h ->
					h.beginTransaction().use {
						val query = if (t == null) insertQuery else updateQuery
						h.mapSql(query)
							.addParameter(tutorialKeyColumn, tutorialKey)
							.addParameter(isShownColumn, true)
							.execute()
					}
				}
			}.unitResponse()
		}

	private fun promiseTutorial(tutorialKey: String): Promise<Tutorial?> =
		DatabasePromise {
			RepositoryAccessHelper(context).use { h ->
				h.beginNonExclusiveTransaction().use {
					h.mapSql("SELECT * FROM Tutorial WHERE tutorialKey = @tutorialKey")
						.addParameter(tutorialKeyColumn, tutorialKey)
						.fetchFirst()
				}
			}
		}
}
