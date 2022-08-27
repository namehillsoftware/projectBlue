package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile

class FakeFileConnectionProvider : FakeConnectionProvider() {
    fun setupFile(serviceFile: ServiceFile, fileProperties: Map<String, String>) {
        mapResponse(
            {
				val returnXml = StringBuilder(
                    """<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<MPL Version="2.0" Title="MCWS - Files - 10936" PathSeparator="\">
<Item>
<Field Name="Key">${serviceFile.key}</Field>
<Field Name="Media Type">Audio</Field>
"""
                )
                for ((key, value) in fileProperties) returnXml.append("<Field Name=\"").append(key)
                    .append("\">").append(
                    value
                ).append("</Field>\n")
                returnXml.append(
                    """
    </Item>
    </MPL>

    """.trimIndent()
                )
                FakeConnectionResponseTuple(200, returnXml.toString().toByteArray())
            },
            "File/GetInfo",
            "File=" + serviceFile.key
        )
    }

    init {
        mapResponse(
            { FakeConnectionResponseTuple(200, ByteArray(0)) },
            "File/GetFile",
            "File=.*",
            "Quality=medium",
            "Conversion=Android",
            "Playback=0"
        )
    }
}
