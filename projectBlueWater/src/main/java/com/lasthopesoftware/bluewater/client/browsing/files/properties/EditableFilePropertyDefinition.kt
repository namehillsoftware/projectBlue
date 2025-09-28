package com.lasthopesoftware.bluewater.client.browsing.files.properties

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
