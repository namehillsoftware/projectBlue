package com.lasthopesoftware.bluewater.client.browsing.files.properties

open class PassThroughFilePropertiesLookup(
	private val listedProperties: Iterable<FileProperty> = emptyList(),
) : LookupFileProperties {
	override val albumArtist: FileProperty?
		get() = get(NormalizedFileProperties.AlbumArtist)
	override val key: FileProperty
		get() = get(NormalizedFileProperties.Key)!!
	override val artist: FileProperty?
		get() = get(NormalizedFileProperties.Artist)
	override val album: FileProperty?
		get() = get(NormalizedFileProperties.Album)
	override val name: FileProperty?
		get() = get(NormalizedFileProperties.Name)
	override val band: FileProperty?
		get() = get(NormalizedFileProperties.Band)
	override val date: FileProperty?
		get() = get(NormalizedFileProperties.Date)
	override val discNumber: FileProperty?
		get() = get(NormalizedFileProperties.DiscNumber)
	override val track: FileProperty?
		get() = get(NormalizedFileProperties.Track)
	override val rating: FileProperty?
		get() = get(NormalizedFileProperties.Rating)
	override val lyrics: FileProperty?
		get() = get(NormalizedFileProperties.Lyrics)
	override val comment: FileProperty?
		get() = get(NormalizedFileProperties.Comment)
	override val composer: FileProperty?
		get() = get(NormalizedFileProperties.Composer)
	override val custom: FileProperty?
		get() = get(NormalizedFileProperties.Custom)
	override val publisher: FileProperty?
		get() = get(NormalizedFileProperties.Publisher)
	override val totalDiscs: FileProperty?
		get() = get(NormalizedFileProperties.TotalDiscs)
	override val genre: FileProperty?
		get() = get(NormalizedFileProperties.Genre)

	override fun get(name: String): FileProperty? = allProperties.firstOrNull { it.name == name }

	override fun update(name: String, value: String) {}

	override val allProperties: Sequence<FileProperty>
		get() = listedProperties.asSequence()
}
