package com.lasthopesoftware.bluewater.shared

import com.lasthopesoftware.resources.io.NonStandardResponseException
import org.jsoup.nodes.Document

class StandardResponse internal constructor(
	status: String?,
	val items: LinkedHashMap<String, String>
) {
    /**
     * @return the status
     */
    val isStatusOk: Boolean = status != null && status.equals("OK", ignoreCase = true)

	companion object {
		private const val responseTagName = "response"
		private const val statusAttrName = "status"
		private const val nameAttrName = "name"

		fun Document.toStandardResponse(): StandardResponse {
			val responseTag = getElementsByTag(responseTagName).singleOrNull() ?: throw NonStandardResponseException()
			val items = responseTag
				.getElementsByTag("item")
				.associate { el -> Pair(el.attr(nameAttrName), el.wholeOwnText()) }
			return StandardResponse(
				responseTag.attr(statusAttrName),
				items as? LinkedHashMap<String, String> ?: LinkedHashMap(items)
			)
		}
	}
}

