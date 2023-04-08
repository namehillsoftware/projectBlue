package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

internal class ItemResponseHandler : DefaultHandler() {
	private var currentKey = ""
	private var currentName = ""
	private var currentPlaylistId: String? = null

	val items: MutableList<Item> = ArrayList()
	var isFailure = false
		private set

	override fun startElement(
		uri: String,
		localName: String,
		qName: String,
		attributes: Attributes
	) {
		if (isFailure) return

		currentKey = ""
		currentName = ""
		currentPlaylistId = null

		if (qName.equals("response", ignoreCase = true)) {
			val status = attributes.getValue("Status")
			if (status.equals("Failure", ignoreCase = true)) {
				isFailure = true;
				return
			}
		}

		if (!"item".equals(qName, ignoreCase = true)) return
		currentName = attributes.getValue("Name")
		currentPlaylistId = attributes.getValue("PlaylistID")
	}

	override fun characters(ch: CharArray, start: Int, length: Int) {
		if (isFailure) return

		currentKey = String(ch, start, length)

	}

	override fun endElement(uri: String, localName: String, qName: String) {
		if (isFailure) return
		if (!"item".equals(qName, ignoreCase = true)) return
		val key = currentKey.toIntOrNull() ?: return
		val item = currentPlaylistId?.toIntOrNull()
			?.let { playlistId -> Item(key, currentName, PlaylistId(playlistId)) }
			?: Item(key, currentName)

		items.add(item)
	}
}
