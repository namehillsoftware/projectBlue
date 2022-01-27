package com.lasthopesoftware.bluewater.client.browsing.library.views.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.ViewType

class SelectStaticViewAdapter(
    context: Context,
    objects: List<String>,
    private val selectedViewType: ViewType?,
    private val selectedViewPosition: Int
) : ArrayAdapter<String>(context, R.layout.layout_select_views, objects)
{
	private val specialViewTypes = arrayOf(ViewType.DownloadView, ViewType.SearchView)
    private val selectViewAdapterBuilder: SelectViewAdapterBuilder = SelectViewAdapterBuilder(context)

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        return selectViewAdapterBuilder.getView(
            convertView,
            parent,
            item,
            specialViewTypes.contains(selectedViewType) && position == selectedViewPosition
        )
    }

}
