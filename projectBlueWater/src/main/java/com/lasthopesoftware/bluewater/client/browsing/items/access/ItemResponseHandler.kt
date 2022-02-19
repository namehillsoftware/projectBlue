package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

internal class ItemResponseHandler : DefaultHandler() {
    private var currentKey = ""
    private var currentName = ""
    private var currentPlaylistId = ""
    @JvmField
	val items: MutableList<Item> = ArrayList()
    override fun startElement(
        uri: String,
        localName: String,
        qName: String,
        attributes: Attributes
    ) {
        currentKey = ""
        currentName = ""
		currentPlaylistId = ""
        if (!"item".equals(qName, ignoreCase = true)) return
        currentName = attributes.getValue("Name")
        currentPlaylistId = attributes.getValue("PlaylistID")
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        currentKey = String(ch, start, length)
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        if (!"item".equals(qName, ignoreCase = true)) return
		val key = currentKey.toIntOrNull() ?: return
        val item = Item(key, currentName)

		item.playlistId = currentPlaylistId.toIntOrNull()
        items.add(item)
    }
}
