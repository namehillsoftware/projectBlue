package com.lasthopesoftware.bluewater.client.browsing.files.properties

enum class EditableFilePropertyDefinition(val propertyName: String, val type: FilePropertyType? = null) {
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
