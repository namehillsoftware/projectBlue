package com.lasthopesoftware.bluewater.client.browsing.items.access

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

object ItemResponse {
	private val logger by lazy { LoggerFactory.getLogger(ItemResponse::class.java) }

    fun getItems(inputStream: InputStream): List<Item> {
        try {
            val sp = SAXParserFactory.newInstance().newSAXParser()
            val jrResponseHandler = ItemResponseHandler()
            sp.parse(inputStream, jrResponseHandler)
            return jrResponseHandler.items
        } catch (e: IOException) {
            logger.error("An error occurred reading the response stream", e)
        } catch (e: ParserConfigurationException) {
            logger.error("An error occurred configuring the parser", e)
        } catch (e: SAXException) {
            logger.error("An error occurred parsing the response", e)
        }
        return ArrayList()
    }
}
