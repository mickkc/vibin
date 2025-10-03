package wtf.ndu.vibin.parsing.parsers.deezer

data class DeezerAlbumMetadata(
    val title: String,
    val cover_big: String?,
    val artist: DeezerArtistMetadata?
)
