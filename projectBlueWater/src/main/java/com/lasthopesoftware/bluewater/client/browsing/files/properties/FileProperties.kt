package com.lasthopesoftware.bluewater.client.browsing.files.properties

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.apache.commons.io.FilenameUtils
import org.joda.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

sealed interface FileProperty : Parcelable {
	val name: String
	val value: String
	val filePropertyType: FilePropertyType
		get() = FilePropertyType.ShortFormText
}

sealed interface ConstantFileProperty : FileProperty

@Parcelize
data class KeyFileProperty(
	override val value: String
) : ConstantFileProperty {
	@IgnoredOnParcel
	override val name = NormalizedFileProperties.Key
}

sealed interface MutableFileProperty : FileProperty

@Parcelize
data class ReadOnlyFileProperty(
	override val name: String,
	override val value: String,
	override val filePropertyType: FilePropertyType = FilePropertyType.ShortFormText,
) : MutableFileProperty, Parcelable

@Parcelize
data class EditableFileProperty(
	override val name: String,
	override val value: String,
	override val filePropertyType: FilePropertyType = FilePropertyType.ShortFormText,
) : MutableFileProperty, Parcelable

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
	private val filePropertyMap by lazy {
		ConcurrentHashMap(
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
			)
		)
	}

	final override val key: FileProperty
		get() = get(NormalizedFileProperties.Key)!!
	final override val artist: FileProperty?
		get() = get(NormalizedFileProperties.Artist)
	final override val albumArtist: FileProperty?
		get() = get(NormalizedFileProperties.AlbumArtist)
	final override val album: FileProperty?
		get() = get(NormalizedFileProperties.Album)
	final override val name: FileProperty?
		get() = get(NormalizedFileProperties.Name)
	final override val band: FileProperty?
		get() = get(NormalizedFileProperties.Band)
	final override val date: FileProperty?
		get() = get(NormalizedFileProperties.Date)
	final override val discNumber: FileProperty?
		get() = get(NormalizedFileProperties.DiscNumber)
	final override val track: FileProperty?
		get() = get(NormalizedFileProperties.Track)
	final override val rating: FileProperty?
		get() = get(NormalizedFileProperties.Rating)
	final override val lyrics: FileProperty?
		get() = get(NormalizedFileProperties.Lyrics)
	final override val comment: FileProperty?
		get() = get(NormalizedFileProperties.Comment)
	final override val composer: FileProperty?
		get() = get(NormalizedFileProperties.Composer)
	final override val custom: FileProperty?
		get() = get(NormalizedFileProperties.Custom)
	final override val publisher: FileProperty?
		get() = get(NormalizedFileProperties.Publisher)
	final override val totalDiscs: FileProperty?
		get() = get(NormalizedFileProperties.TotalDiscs)
	final override val genre: FileProperty?
		get() = get(NormalizedFileProperties.Genre)

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

	final override fun get(name: String): FileProperty? = filePropertyMap
		.getOrElse(name, { lazyFileProperty(name) })
		.value

	final override fun update(name: String, value: String) {
		updateValue(name, value)
		filePropertyMap.put(name, lazyFileProperty(name))
	}

	protected abstract val availableProperties: Set<String>

	protected abstract fun getValue(name: String): String?

	protected abstract fun updateValue(name: String, value: String)

	protected abstract fun isEditable(name: String): Boolean

	private fun lazyAssociation(name: String): Pair<String, Lazy<FileProperty?>> =
		Pair(name, lazyFileProperty(name))

	private fun lazyFileProperty(name: String) = lazy {
		val isEditable = isEditable(name)
		val filePropertyType = FilePropertyDefinition.fromName(name)?.type ?: FilePropertyType.ShortFormText

		val value = getValue(name) ?: if (!isEditable) return@lazy null else when (filePropertyType) {
			FilePropertyType.Integer -> "0"
			else -> ""
		}

		when {
			name == NormalizedFileProperties.Key -> KeyFileProperty(value)
			isEditable(name) -> EditableFileProperty(name, value, filePropertyType)
			else -> ReadOnlyFileProperty(name, value, filePropertyType)
		}
	}
}

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

	val FileProperty.editableFilePropertyDefinition: FilePropertyDefinition.EditableFilePropertyDefinition?
		get() = FilePropertyDefinition.EditableFilePropertyDefinition.fromName(name)

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

sealed interface FilePropertyDefinition {
	val propertyName: String
	val type: FilePropertyType

	enum class ReadOnlyFilePropertyDefinition(override val propertyName: String, override val type: FilePropertyType): FilePropertyDefinition {
		LastPlayed(NormalizedFileProperties.LastPlayed, FilePropertyType.Date);

		override fun toString() = propertyName
	}

	enum class EditableFilePropertyDefinition(override val propertyName: String, override val type: FilePropertyType) : FilePropertyDefinition {
		Artist(NormalizedFileProperties.Artist, FilePropertyType.ShortFormText),
		AlbumArtist(NormalizedFileProperties.AlbumArtist, FilePropertyType.ShortFormText),
		Album(NormalizedFileProperties.Album, FilePropertyType.ShortFormText),
		Band(NormalizedFileProperties.Band, FilePropertyType.ShortFormText),
		Date(NormalizedFileProperties.Date, FilePropertyType.Date),
		DiscNumber(NormalizedFileProperties.DiscNumber, FilePropertyType.Integer),
		Name(NormalizedFileProperties.Name, FilePropertyType.ShortFormText),
		Track(NormalizedFileProperties.Track, FilePropertyType.Integer),
		Rating(NormalizedFileProperties.Rating, FilePropertyType.Integer),
		Lyrics(NormalizedFileProperties.Lyrics, FilePropertyType.LongFormText),
		Comment(NormalizedFileProperties.Comment, FilePropertyType.LongFormText),
		Composer(NormalizedFileProperties.Composer, FilePropertyType.ShortFormText),
		Custom(NormalizedFileProperties.Custom, FilePropertyType.LongFormText),
		Publisher(NormalizedFileProperties.Publisher, FilePropertyType.ShortFormText),
		TotalDiscs(NormalizedFileProperties.TotalDiscs, FilePropertyType.Integer),
		Genre(NormalizedFileProperties.Genre, FilePropertyType.ShortFormText);

		override fun toString() = propertyName

		companion object {
			private val propertyLookup by lazy { entries.associateBy { fp -> fp.propertyName } }

			fun fromName(name: String) = propertyLookup[name]
		}
	}

	companion object {
		private val propertyLookup by lazy {
			(ReadOnlyFilePropertyDefinition.entries + EditableFilePropertyDefinition.entries).associate { it.propertyName to it as FilePropertyDefinition }
		}

		fun fromName(name: String) = propertyLookup[name]
	}
}

enum class FilePropertyType {
	ShortFormText, LongFormText, Date, Integer,
}
