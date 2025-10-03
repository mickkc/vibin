package wtf.ndu.vibin.parsing.parsers.theaudiodb

import kotlinx.serialization.Serializable

@Serializable
data class TadbArtist (
    val strArtist: String,
    val strArtistThumb: String?,
    val strBiographyEN: String?,
    val strBiographyDE: String?,
    val strBiographyFR: String?,
    val strBiographyCN: String?,
    val strBiographyIT: String?,
    val strBiographyJP: String?,
    val strBiographyRU: String?,
    val strBiographyES: String?,
    val strBiographyPT: String?,
    val strBiographySE: String?,
    val strBiographyNL: String?,
    val strBiographyHU: String?,
    val strBiographyNO: String?,
    val strBiographyIL: String?,
    val strBiographyPL: String?
)

@Serializable
data class TadbArtistResponse (
    val artists: List<TadbArtist>
)