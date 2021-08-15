package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.app.Activity
import android.os.Build
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.IItemListMenuChangeHandler
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.IFileListParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.FileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import tourguide.tourguide.Overlay
import tourguide.tourguide.Pointer
import tourguide.tourguide.ToolTip
import tourguide.tourguide.TourGuide

class DemoableItemListAdapter(
	private val activity: Activity,
	resource: Int,
	private val items: List<Item>,
	fileListParameterProvider: IFileListParameterProvider,
	fileStringListProvider: FileStringListProvider,
	itemListMenuEvents: IItemListMenuChangeHandler,
	storedItemAccess: StoredItemAccess,
	library: Library
) : ItemListAdapter(
	activity,
	resource,
	items,
	fileListParameterProvider,
	fileStringListProvider,
	itemListMenuEvents,
	storedItemAccess,
	library
) {
	private var wasTutorialShown = false

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = super.getView(position, convertView, parent)
		if (items.isEmpty() || items.size > 1 && position != 2) return view
		buildTutorialView(view)
		return view
	}

	private fun buildTutorialView(view: View) {
		// use this flag to ensure the least amount of possible work is done for this tutorial
		if (wasTutorialShown) return

		wasTutorialShown = true

		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
		if (!DEBUGGING_TUTORIAL && sharedPreferences.getBoolean(isListTutorialShownPreference, false)) return
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

		sharedPreferences.edit().putBoolean(isListTutorialShownPreference, true).apply()
	}

	companion object {
		private const val isListTutorialShownPreference = "isListTutorialShownPreference"
		private const val DEBUGGING_TUTORIAL = false
	}
}
