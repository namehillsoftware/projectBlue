package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.RevisionChecker
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

class FilePropertiesStorage(
	private val connectionProvider: IConnectionProvider,
	private val filePropertiesContainerRepository: IFilePropertiesContainerRepository
) {
	fun promiseFileUpdate(
		serviceFile: ServiceFile,
		property: String,
		value: String,
		isFormatted: Boolean
	): Promise<Unit> {
		val promisedUpdate = connectionProvider
			.promiseResponse(
				"File/SetInfo",
				"File=" + serviceFile.key.toString(),
				"Field=$property",
				"Value=$value",
				"formatted=" + if (isFormatted) "1" else "0"
			)
			.then { response ->
				try {
					val responseCode = response.code
					logger.info("api/v1/File/SetInfo responded with a response code of $responseCode")
				} finally {
					response.body?.close()
				}
			}

		promisedUpdate.eventually {	RevisionChecker.promiseRevision(connectionProvider)	}
			.then { revision ->
				val urlKeyHolder = UrlKeyHolder(connectionProvider.urlProvider.baseUrl, serviceFile)
				val filePropertiesContainer = filePropertiesContainerRepository.getFilePropertiesContainer(urlKeyHolder)
				if (filePropertiesContainer!!.revision == revision) filePropertiesContainer.updateProperty(
					property,
					value
				)
			}
			.excuse { e ->
				logger.warn(
					serviceFile.key.toString() + "'s property cache item " + property + " was not updated with the new value of " + value,
					e
				)
			}
		return promisedUpdate
	}

	companion object {
		private val logger = LoggerFactory.getLogger(FilePropertiesStorage::class.java)
		fun storeFileProperty(
			connectionProvider: IConnectionProvider,
			filePropertiesContainerRepository: IFilePropertiesContainerRepository,
			serviceFile: ServiceFile,
			property: String,
			value: String,
			isFormatted: Boolean
		) {
			FilePropertiesStorage(connectionProvider, filePropertiesContainerRepository).promiseFileUpdate(
				serviceFile,
				property,
				value,
				isFormatted
			)
		}
	}
}
