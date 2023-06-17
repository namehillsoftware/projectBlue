package com.lasthopesoftware.bluewater.shared

import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

class StandardResponse internal constructor(status: String?) {
    /**
     * @return the status
     */
    val isStatus: Boolean
    val items = HashMap<String?, String?>()

    init {
        isStatus = status != null && status.equals("OK", ignoreCase = true)
    }

    companion object {
		private val logger by lazyLogger<StandardResponse>()

        fun fromInputStream(`is`: InputStream?): StandardResponse? {
            try {
                val sp = SAXParserFactory.newInstance().newSAXParser()
                val jrResponseHandler = StandardResponseHandler()
                sp.parse(`is`, jrResponseHandler)
                return jrResponseHandler.response
            } catch (e: Exception) {
                logger.error("An error occurred parsing the input stream", e)
            }
            return null
        }
    }
}
