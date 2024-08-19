package com.lasthopesoftware.bluewater.shared

object XmlParsingHelpers {
    fun handleBadXml(currentSb: StringBuilder?, ch: CharArray?, start: Int, length: Int) {
        currentSb?.appendRange(ch!!, start, start + length)
    }
}
