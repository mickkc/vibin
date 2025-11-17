package wtf.ndu.vibin.parsing.parsers.deezer

data class DeezerAlbumMetadata(
    val title: String,
    val cover_xl: String?,
    val artist: DeezerArtistMetadata?,
    val record_type: String?,
)
