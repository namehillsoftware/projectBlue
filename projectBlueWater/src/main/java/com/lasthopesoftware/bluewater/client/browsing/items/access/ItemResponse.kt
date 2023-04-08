package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

object ItemResponse {
	fun getItems(inputStream: InputStream): List<Item> {
		val sp = SAXParserFactory.newInstance().newSAXParser()
		val jrResponseHandler = ItemResponseHandler()
		sp.parse(inputStream, jrResponseHandler)
		if (jrResponseHandler.isFailure)
			throw IOException("Server returned 'Failure'.")
		return jrResponseHandler.items
	}
}
