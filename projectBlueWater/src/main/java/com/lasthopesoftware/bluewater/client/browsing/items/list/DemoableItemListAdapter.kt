package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.app.Activity
import android.os.Build
import android.view.View
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.tutorials.ManageTutorials
import com.lasthopesoftware.bluewater.tutorials.TutorialManager
import tourguide.tourguide.Overlay
import tourguide.tourguide.Pointer
import tourguide.tourguide.ToolTip
import tourguide.tourguide.TourGuide

class DemoableItemListAdapter
(
	private val activity: Activity,
	private val sendMessages: SendMessages,
	fileListParameterProvider: IFileListParameterProvider,
	fileStringListProvider: FileStringListProvider,
	itemListMenuEvents: IItemListMenuChangeHandler,
	storedItemAccess: StoredItemAccess,
	provideItems: ProvideItems,
	library: Library,
	private val manageTutorials: ManageTutorials
) : ItemListAdapter(
	activity,
	sendMessages,
	fileListParameterProvider,
	fileStringListProvider,
	itemListMenuEvents,
	storedItemAccess,
	provideItems,
	library
) {
	private var wasTutorialShown = false

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		super.onBindViewHolder(holder, position)
		if (itemCount == 1 || position != 2)
			bindTutorialView(holder.itemView)
	}

	private fun bindTutorialView(view: View) {
		// use this flag to ensure the least amount of possible work is done for this tutorial
		if (wasTutorialShown) return

		wasTutorialShown = true

		fun showTutorial() {
			val displayColor =
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)	activity.resources.getColor(R.color.clearstream_blue, null)
				else activity.resources.getColor(R.color.clearstream_blue)

			val tourGuide = TourGuide.init(activity).with(TourGuide.Technique.CLICK)
				.setPointer(Pointer().setColor(displayColor))
				.setToolTip(
					ToolTip()
						.setTitle(activity.getString(R.string.title_long_click_menu))
						.setDescription(activity.getString(R.string.tutorial_long_click_menu))
						.setBackgroundColor(displayColor)
				)
				.setOverlay(Overlay())
				.playOn(view)

			view.setOnLongClickListener {
				tourGuide.cleanUp()
				view.setOnLongClickListener(null)
				false
			}
		}

		if (DEBUGGING_TUTORIAL) {
			showTutorial()
			return
		}

		manageTutorials
			.promiseWasTutorialShown(TutorialManager.KnownTutorials.longPressListTutorial)
			.eventually(LoopedInPromise.response({ wasShown ->
				if (!wasShown) {
					showTutorial()
					manageTutorials.promiseTutorialMarked(TutorialManager.KnownTutorials.longPressListTutorial)
				}
			}, activity))
	}

	companion object {
		private const val DEBUGGING_TUTORIAL = false
	}
}
