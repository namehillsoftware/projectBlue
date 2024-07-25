package com.lasthopesoftware.bluewater.client.browsing.files.properties

enum class EditableFilePropertyDefinition(val propertyName: String, val type: FilePropertyType? = null) {
	Artist(KnownFileProperties.Artist, FilePropertyType.ShortFormText),
	AlbumArtist(KnownFileProperties.AlbumArtist, FilePropertyType.ShortFormText),
	Album(KnownFileProperties.Album, FilePropertyType.ShortFormText),
	Band(KnownFileProperties.Band, FilePropertyType.ShortFormText),
	Date(KnownFileProperties.Date, FilePropertyType.Date),
	DiscNumber(KnownFileProperties.DiscNumber, FilePropertyType.Integer),
	Name(KnownFileProperties.Name, FilePropertyType.ShortFormText),
	Track(KnownFileProperties.Track, FilePropertyType.Integer),
	Rating(KnownFileProperties.Rating, FilePropertyType.Integer),
	Lyrics(KnownFileProperties.Lyrics, FilePropertyType.LongFormText),
	Comment(KnownFileProperties.Comment, FilePropertyType.LongFormText),
	Composer(KnownFileProperties.Composer, FilePropertyType.ShortFormText),
	Custom(KnownFileProperties.Custom, FilePropertyType.LongFormText),
	Publisher(KnownFileProperties.Publisher, FilePropertyType.ShortFormText),
	TotalDiscs(KnownFileProperties.TotalDiscs, FilePropertyType.Integer),
	Genre(KnownFileProperties.Genre, FilePropertyType.ShortFormText);

	override fun toString() = propertyName

	companion object {
		private val propertyLookup by lazy { entries.associateBy { fp -> fp.propertyName } }

		fun fromName(name: String) = propertyLookup[name]
	}
}
