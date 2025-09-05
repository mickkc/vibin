package wtf.ndu.vibin.dto.artists

data class ArtistEditData (
    val name: String?,
    val sortName: String?,
    val imageUrl: String?,
    val tagIds: List<Long>? = null
)