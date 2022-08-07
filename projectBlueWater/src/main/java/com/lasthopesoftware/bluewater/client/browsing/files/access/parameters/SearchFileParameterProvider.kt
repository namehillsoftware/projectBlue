package com.lasthopesoftware.bluewater.client.browsing.files.access.parameters

object SearchFileParameterProvider {
    fun getFileListParameters(query: String) =
		arrayOf("Files/Search", "Query=[Media Type]=[Audio] $query")
}
