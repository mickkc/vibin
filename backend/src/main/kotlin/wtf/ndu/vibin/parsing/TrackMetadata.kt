package wtf.ndu.vibin.parsing

data class TrackMetadata (
    val title: String,
    val artistNames: List<String>?,
    val albumName: String?,
    val trackNumber: Int? = null,
    val trackCount: Int? = null,
    val discNumber: Int? = null,
    val discCount: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val durationMs: Long? = null,
    val comment: String? = null,
    val coverImageData: ByteArray? = null,
    val explicit: Boolean? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackMetadata

        if (trackNumber != other.trackNumber) return false
        if (discNumber != other.discNumber) return false
        if (year != other.year) return false
        if (durationMs != other.durationMs) return false
        if (title != other.title) return false
        if (artistNames != other.artistNames) return false
        if (albumName != other.albumName) return false
        if (genre != other.genre) return false
        if (comment != other.comment) return false
        if (!coverImageData.contentEquals(other.coverImageData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackNumber ?: 0
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (year ?: 0)
        result = 31 * result + (durationMs?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (artistNames?.hashCode() ?: 0)
        result = 31 * result + (albumName?.hashCode() ?: 0)
        result = 31 * result + (genre?.hashCode() ?: 0)
        result = 31 * result + (comment?.hashCode() ?: 0)
        result = 31 * result + (coverImageData?.contentHashCode() ?: 0)
        return result
    }
}