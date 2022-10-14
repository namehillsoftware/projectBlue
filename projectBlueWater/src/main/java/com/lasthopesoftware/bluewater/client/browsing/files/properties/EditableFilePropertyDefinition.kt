package com.lasthopesoftware.bluewater.client.browsing.files.properties

enum class EditableFilePropertyDefinition(val descriptor: String, val type: FilePropertyType? = null) {
	Artist(KnownFileProperties.Artist, FilePropertyType.ShortFormText),
	AlbumArtist(KnownFileProperties.AlbumArtist, FilePropertyType.ShortFormText),
	Album(KnownFileProperties.Album, FilePropertyType.ShortFormText),
	Date(KnownFileProperties.Date, FilePropertyType.Date),
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

	override fun toString() = descriptor

	companion object {
		private val descriptorLookup by lazy { values().associateBy { fp -> fp.descriptor } }

		fun fromDescriptor(descriptor: String) = descriptorLookup[descriptor]
	}
}
