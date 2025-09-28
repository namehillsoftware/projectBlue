package com.lasthopesoftware.bluewater.client.browsing.files.properties

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.apache.commons.io.FilenameUtils
import org.joda.time.Duration
import java.util.regex.Pattern

interface FileProperty : Parcelable {
	val name: String
	val value: String
	val filePropertyType: FilePropertyType
		get() = FilePropertyType.ShortFormText
}

interface LookupFileProperties {
	val key: FileProperty
	val artist: FileProperty?
	val albumArtist: FileProperty?
	val album: FileProperty?
	val name: FileProperty?
	val band: FileProperty?
	val date: FileProperty?
	val discNumber: FileProperty?
	val track: FileProperty?
	val rating: FileProperty?
	val lyrics: FileProperty?
	val comment: FileProperty?
	val composer: FileProperty?
	val custom: FileProperty?
	val publisher: FileProperty?
	val totalDiscs: FileProperty?
	val genre: FileProperty?
	fun get(name: String): FileProperty?
	fun update(name: String, value: String)
	val allProperties: Sequence<FileProperty>
}

abstract class FilePropertiesLookup : LookupFileProperties {
	private val lazyNullFileProperty: Lazy<FileProperty?> = lazy { null }

	private val filePropertyMap by lazy {
		mapOf(
			lazyAssociation(NormalizedFileProperties.Key),
			lazyAssociation(NormalizedFileProperties.Name),
			lazyAssociation(NormalizedFileProperties.Artist),
			lazyAssociation(NormalizedFileProperties.AlbumArtist),
			lazyAssociation(NormalizedFileProperties.Album),
			lazyAssociation(NormalizedFileProperties.Band),
			lazyAssociation(NormalizedFileProperties.Date),
			lazyAssociation(NormalizedFileProperties.DiscNumber),
			lazyAssociation(NormalizedFileProperties.Track),
			lazyAssociation(NormalizedFileProperties.Rating),
			lazyAssociation(NormalizedFileProperties.Lyrics),
			lazyAssociation(NormalizedFileProperties.Comment),
			lazyAssociation(NormalizedFileProperties.Composer),
			lazyAssociation(NormalizedFileProperties.Custom),
			lazyAssociation(NormalizedFileProperties.Publisher),
			lazyAssociation(NormalizedFileProperties.TotalDiscs),
			lazyAssociation(NormalizedFileProperties.Genre),
		).withDefault { lazyNullFileProperty }
	}

	final override val key: FileProperty by lazy { filePropertyMap.getValue(NormalizedFileProperties.Key).value!! }
	final override val artist: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Artist)
	final override val albumArtist: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.AlbumArtist)
	final override val album: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Album)
	final override val name: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Name)
	final override val band: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Band)
	final override val date: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Date)
	final override val discNumber: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.DiscNumber)
	final override val track: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Track)
	final override val rating: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Rating)
	final override val lyrics: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Lyrics)
	final override val comment: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Comment)
	final override val composer: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Composer)
	final override val custom: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Custom)
	final override val publisher: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Publisher)
	final override val totalDiscs: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.TotalDiscs)
	final override val genre: FileProperty? by filePropertyMap.getValue(NormalizedFileProperties.Genre)

	final override val allProperties: Sequence<FileProperty>
		get() = sequence {
			val returnedProperties = HashSet<String>()
			for ((key, lazyFileProperty) in filePropertyMap) {
				val maybeProperty = lazyFileProperty.value
				if (maybeProperty != null && returnedProperties.add(key))
					yield(maybeProperty)
			}

			for (key in availableProperties) {
				if (returnedProperties.contains(key)) continue

				val maybeProperty = get(key)
				if (maybeProperty != null && returnedProperties.add(key))
					yield(maybeProperty)
			}
		}

	final override fun get(name: String): FileProperty? {
		val isEditable = isEditable(name)
		val filePropertyType = EditableFilePropertyDefinition.fromName(name)?.type ?: FilePropertyType.ShortFormText

		val value = getValue(name) ?: if (!isEditable) return null else when (filePropertyType) {
			FilePropertyType.Integer -> "0"
			else -> ""
		}

		return if (isEditable(name)) EditableFileProperty(name, value, filePropertyType) else ReadOnlyFileProperty(name, value, filePropertyType)
	}

	protected abstract val availableProperties: Set<String>

	protected abstract fun getValue(name: String): String?

	protected abstract fun isEditable(name: String): Boolean

	private fun lazyAssociation(name: String): Pair<String, Lazy<FileProperty?>> =
		Pair(name, lazyFileProperty(name))

	private fun lazyFileProperty(name: String): Lazy<FileProperty?> = lazy { get(name) }
}

@Parcelize
data class ReadOnlyFileProperty(
	override val name: String,
	override val value: String,
	override val filePropertyType: FilePropertyType = FilePropertyType.ShortFormText,
) : FileProperty, Parcelable

@Parcelize
data class EditableFileProperty(
	override val name: String,
	override val value: String,
	override val filePropertyType: FilePropertyType = FilePropertyType.ShortFormText,
) : FileProperty, Parcelable

object FilePropertyHelpers {

	private val reservedCharactersPattern by lazy { Pattern.compile("[|?*<\":>+\\[\\]'/]") }

	private fun String.replaceReservedCharsAndPath(): String =
		reservedCharactersPattern.matcher(this).replaceAll("_")

	data class FileNameParts(
		val directory: String,
		val baseFileName: String,
		val ext: String,
		val postExtension: String
	)

	val FileProperty.editableFilePropertyDefinition: EditableFilePropertyDefinition?
		get() = EditableFilePropertyDefinition.fromName(name)

	val LookupFileProperties.durationInMs: Long?
		get() = this.get(NormalizedFileProperties.Duration)?.value?.toDoubleOrNull()?.let { it * 1000 }?.toLong()

	val LookupFileProperties.duration: Duration?
		get() = durationInMs?.let(Duration::millis)

	val LookupFileProperties.albumArtistOrArtist
		get() = this.albumArtist ?: this.artist

	val LookupFileProperties.fileNameParts
		get() = this.get(NormalizedFileProperties.Filename)
			?.value
			?.let { f ->
				val path = FilenameUtils.getPath(f)
				val fileName = FilenameUtils.getName(f)

				var baseName = fileName
				var ext = ""
				val extensionIndex = fileName.lastIndexOf('.')
				if (extensionIndex > -1) {
					baseName = fileName.substring(0, extensionIndex)
					ext = fileName.substring(extensionIndex + 1)
				}

				val postExtParts = ext.substringAfter(';', "")

				FileNameParts(path, baseName, ext, postExtParts)
			}

	val LookupFileProperties.baseFileNameAsMp3
		get() = fileNameParts
			?.run {
				if (postExtension.isNotEmpty()) "$postExtension.mp3"
				else "$baseFileName.mp3"
			}

	val LookupFileProperties.localExternalRelativeFileDirectory
		get() = albumArtistOrArtist?.value?.trim { c -> c <= ' ' }?.replaceReservedCharsAndPath()
			?.let { path ->
				this.album
					?.value
					?.let { album ->
						FilenameUtils.concat(
							path, album.trim { it <= ' ' }.replaceReservedCharsAndPath()
						)
					}
					?: path
			}
			?.let { path -> fileNameParts?.takeIf { it.postExtension.isNotEmpty() }?.run { FilenameUtils.concat(path, baseFileName) } ?: path }
			?.let { path -> if (!path.endsWith("/")) "$path/" else path }

	val LookupFileProperties.localExternalRelativeFilePathAsMp3
		get() = localExternalRelativeFileDirectory?.let { dir ->
			baseFileNameAsMp3?.let { mp3 ->
					FilenameUtils.concat(dir, mp3).trim { it <= ' ' }
				}
			}

}
