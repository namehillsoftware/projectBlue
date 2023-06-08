package com.lasthopesoftware.bluewater.shared

import org.slf4j.LoggerFactory
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

class StandardRequest internal constructor(status: String?) {
    /**
     * @return the status
     */
    val isStatus: Boolean
    val items = HashMap<String?, String?>()

    init {
        isStatus = status != null && status.equals("OK", ignoreCase = true)
    }

    companion object {
        fun fromInputStream(`is`: InputStream?): StandardRequest? {
            try {
                val sp = SAXParserFactory.newInstance().newSAXParser()
                val jrResponseHandler = StandardResponseHandler()
                sp.parse(`is`, jrResponseHandler)
                return jrResponseHandler.response
            } catch (e: Exception) {
                LoggerFactory.getLogger(StandardRequest::class.java).error(e.toString(), e)
            }
            return null
        }
    }
}
