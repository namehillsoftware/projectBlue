package com.lasthopesoftware.bluewater.tutorials

import android.content.Context
import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper
import com.lasthopesoftware.bluewater.repository.fetchFirst
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tableName
import com.lasthopesoftware.bluewater.tutorials.DisplayedTutorialEntityInformation.tutorialKeyColumn
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools.promiseTableMessage
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.querydroid.SqLiteAssistants

class TutorialManager(private val context: Context, private val tutorialCache: CacheTutorialInformation = TutorialInformationCache) : ManageTutorials {
	object KnownTutorials {
		const val longPressListTutorial = "longPressListTutorial"
		const val adjustNotificationInApplicationSettingsTutorial = "adjustNotificationInApplicationSettings"
	}

	companion object {
		private val insertQuery by lazy {
			SqLiteAssistants.InsertBuilder.fromTable(tableName)
				.addColumn(tutorialKeyColumn)
				.buildQuery()
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
			else promiseTableMessage<Unit> {
				RepositoryAccessHelper(context).use { h ->
					h.beginTransaction().use {
						h.mapSql(insertQuery)
							.addParameter(tutorialKeyColumn, tutorialKey)
							.execute()
						it.setTransactionSuccessful()
					}
				}
			}.then { _ ->
				tutorialCache[tutorialKey] = true
			}
		}

	private fun promiseTutorial(tutorialKey: String): Promise<DisplayedTutorial?> =
		promiseTableMessage<DisplayedTutorial?> {
			RepositoryAccessHelper(context).use { h ->
				h.beginNonExclusiveTransaction().use {
					h.mapSql("SELECT * FROM $tableName WHERE $tutorialKeyColumn = @$tutorialKeyColumn")
						.addParameter(tutorialKeyColumn, tutorialKey)
						.fetchFirst()
				}
			}
		}
}
