package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.ProvideImages
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideEditableScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.UpdateScopedFileProperties
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideScopedUrlKey
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import org.joda.time.DateTime
import org.joda.time.Duration

class FileDetailsPreviewProvider : PreviewParameterProvider<FileDetailsViewModel> {
	override val values: Sequence<FileDetailsViewModel>
		get() = sequenceOf(
			FileDetailsViewModel(
				object : CheckIfScopedConnectionIsReadOnly {
					override fun promiseIsReadOnly(): Promise<Boolean> {
						return true.toPromise()
					}
				},
				object : ProvideEditableScopedFileProperties {
					private val duration = Duration.standardMinutes(5).millis
					private val lastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds

					override fun promiseFileProperties(serviceFile: ServiceFile) = Promise(
						sequenceOf(
							FileProperty("Key", "23"),
							FileProperty("Media Type", "Audio"),
							FileProperty(KnownFileProperties.LastPlayed, lastPlayed.toString()),
							FileProperty("Rating", "4"),
							FileProperty("File Size", "2345088"),
							FileProperty(KnownFileProperties.Duration, duration.toString()),
						)
					)
				},
				object : UpdateScopedFileProperties {
					override fun promiseFileUpdate(
						serviceFile: ServiceFile,
						property: String,
						value: String,
						isFormatted: Boolean
					): Promise<Unit> {
						TODO("Not yet implemented")
					}
				},
				object : ProvideDefaultImage {
					override fun promiseFileBitmap(): Promise<Bitmap> =
						Promise(BitmapFactory.decodeByteArray(ByteArray(0), 0, 0))
				},
				object : ProvideImages {
					override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
						Promise(BitmapFactory.decodeByteArray(ByteArray(0), 0, 0))
				},
				object : ControlPlaybackService {
					override fun promiseIsMarkedForPlay(): Promise<Boolean> {
						TODO("Not yet implemented")
					}

					override fun play() {
						TODO("Not yet implemented")
					}

					override fun pause() {
						TODO("Not yet implemented")
					}

					override fun startPlaylist(fileStringList: String, position: Int) {
						TODO("Not yet implemented")
					}

					override fun startPlaylist(serviceFiles: List<ServiceFile>, position: Int) {
						TODO("Not yet implemented")
					}

					override fun addToPlaylist(serviceFile: ServiceFile) {
						TODO("Not yet implemented")
					}

					override fun setRepeating() {
						TODO("Not yet implemented")
					}

					override fun setCompleting() {
						TODO("Not yet implemented")
					}
				},
			object : RegisterForApplicationMessages {
				override fun <Message : ApplicationMessage> registerForClass(
					messageClass: Class<Message>,
					receiver: (Message) -> Unit
				): AutoCloseable {
					TODO("Not yet implemented")
				}

				override fun <Message : ApplicationMessage> unregisterReceiver(receiver: (Message) -> Unit) {
					TODO("Not yet implemented")
				}

			},
			object : ProvideScopedUrlKey {
				override fun <Key> promiseUrlKey(key: Key): Promise<UrlKeyHolder<Key>?> {
					TODO("Not yet implemented")
				}
			}
		),
	)
}
