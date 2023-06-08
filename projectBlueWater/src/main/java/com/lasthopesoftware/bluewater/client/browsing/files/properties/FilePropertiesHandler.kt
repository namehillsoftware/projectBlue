package com.lasthopesoftware.bluewater.client.browsing.files.properties

import com.lasthopesoftware.bluewater.shared.XmlParsingHelpers
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.util.TreeMap

internal class FilePropertiesHandler : DefaultHandler() {
    /**
     * @return the response
     */
    val properties = TreeMap<String?, String>(java.lang.String.CASE_INSENSITIVE_ORDER)
    private var currentSb: StringBuilder? = null
    private var currentKey: String? = null
    override fun startElement(
        uri: String,
        localName: String,
        qName: String,
        attributes: Attributes
    ) {
        currentSb = StringBuilder()
        if (qName.equals("field", ignoreCase = true)) currentKey = attributes.getValue("Name")
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        XmlParsingHelpers.handleBadXml(currentSb, ch, start, length)
    }

    override fun endElement(uri: String, localName: String, qName: String) {
//		if (sb != null) currentValue = sb.toString();
        if (qName.equals("field", ignoreCase = true)) {
            properties[currentKey] = currentSb.toString()
        }
    }
}
