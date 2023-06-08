package com.lasthopesoftware.bluewater.shared

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

internal class StandardResponseHandler : DefaultHandler() {
    /**
     * @return the response
     */
    var response: StandardRequest? = null
        private set
    private var currentValue: String? = null
    private var currentKey: String? = null
    @Throws(SAXException::class)
    override fun startElement(
        uri: String,
        localName: String,
        qName: String,
        attributes: Attributes
    ) {
        currentValue = ""
        currentKey = ""
        if (qName.equals("response", ignoreCase = true)) response =
            StandardRequest(attributes.getValue("Status"))
        if (qName.equals("item", ignoreCase = true)) {
            currentKey = attributes.getValue("Name")
        }
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        currentValue = String(ch, start, length)
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String, localName: String, qName: String) {
        if (qName.equals("item", ignoreCase = true)) response!!.items[currentKey] = currentValue
    }
}
