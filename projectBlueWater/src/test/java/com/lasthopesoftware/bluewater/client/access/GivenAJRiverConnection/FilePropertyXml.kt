package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile

fun buildFilePropertiesXml(serviceFile: ServiceFile, fileProperties: Map<String, String>): String {
	val returnXml = StringBuilder(
		"""<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<MPL Version="2.0" Title="MCWS - Files - 10936" PathSeparator="\">
<Item>
<Field Name="Key">${serviceFile.key}</Field>
<Field Name="Media Type">Audio</Field>
"""
	)

	for ((key, value) in fileProperties) {
		returnXml
			.append("<Field Name=\"")
			.append(key)
			.append("\">")
			.append(value)
			.append("</Field>\n")
	}

	returnXml
		.append(
			"""
    </Item>
    </MPL>

    """.trimIndent()
		)

	return returnXml.toString()
}
