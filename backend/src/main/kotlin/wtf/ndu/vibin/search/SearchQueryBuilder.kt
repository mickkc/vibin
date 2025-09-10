package wtf.ndu.vibin.search

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import wtf.ndu.vibin.db.albums.AlbumTable
import wtf.ndu.vibin.db.artists.ArtistTable
import wtf.ndu.vibin.db.artists.TrackArtistConnection
import wtf.ndu.vibin.db.tags.TagTable
import wtf.ndu.vibin.db.tags.TrackTagConnection
import wtf.ndu.vibin.db.tracks.TrackTable

object SearchQueryBuilder {

    val trueBooleans = listOf("1", "true", "yes", "on")
    val falseBooleans = listOf("0", "false", "no", "off")

    fun build(queryString: String): Op<Boolean> {
        val parts = split(queryString)
        return buildQuery(parts)
    }

    fun split(queryString: String): List<String> {

        val parts = mutableListOf<String>()
        var current = ""
        var isEscaped = false
        var inQuotes = false
        var bracketDepth = 0

        for (i in 0 until queryString.length) {

            if (queryString[i] == '(' && !isEscaped && !inQuotes) {
                bracketDepth++
                current += queryString[i]
                continue
            }

            if (bracketDepth > 0) {
                if (queryString[i] == ')') {
                    bracketDepth--
                    current += queryString[i]
                    if (bracketDepth == 0) {
                        parts.add(current.trim())
                        current = ""
                    }
                    continue
                } else {
                    current += queryString[i]
                    continue
                }
            }

            if (queryString[i] == '\\' && !isEscaped) {
                isEscaped = true
                continue
            }
            if (queryString[i] == '"' && !isEscaped) {
                inQuotes = !inQuotes
                current += queryString[i]
                if (!inQuotes) {
                    parts.add(current.trim())
                    current = ""
                }
            } else if (queryString[i].isWhitespace() && !inQuotes) {
                if (current.isNotBlank()) {
                    parts.add(current.trim())
                    current = ""
                }
            } else {
                current += queryString[i]
            }

            isEscaped = false
        }

        if (current.isNotBlank()) {
            parts.add(current.trim())
        }

        if (inQuotes) {
            throw IllegalArgumentException("Unclosed quotes in query")
        }

        if (bracketDepth > 0) {
            throw IllegalArgumentException("Unclosed brackets in query")
        }

        return parts
    }

    fun buildQuery(parts: List<String>): Op<Boolean> {

        val pparts = parts.toMutableList()
        var op: Op<Boolean>? = null

        fun addWithRelation(nop: Op<Boolean>, relation: String) {
            op = if (op == null) {
                nop
            } else {
                when (relation) {
                    "AND" -> op!! and nop
                    "OR" -> op!! or nop
                    else -> throw IllegalArgumentException("Unknown relation: $relation")
                }
            }
        }

        while (pparts.isNotEmpty()) {

            var relationPart: String? = pparts.removeFirst()
            val opPart: String = if (relationPart in listOf("AND", "OR")) {
                pparts.removeFirst()
            } else {
                val b = relationPart!!
                relationPart = null
                b
            }

            // region Parentheses
            if (opPart.startsWith("(") && opPart.endsWith(")")) {
                val parts = split(opPart.removePrefix("(").removeSuffix(")"))
                val nop = buildQuery(parts)
                addWithRelation(nop, relationPart ?: "AND")
            }
            // endregion
            // region Title
            else if (opPart.startsWith("t:")) {
                val titleSearch = opPart.removePrefix("t:").removeSurrounding("\"")
                val nop = titleSearch(titleSearch)
                addWithRelation(nop, relationPart ?: "AND")
            }
            // endregion
            // region Artist
            else if (opPart.startsWith("a:")) {
                val artistSearch = opPart.removePrefix("a:").removeSurrounding("\"")
                val nop = artistSearch(artistSearch)
                addWithRelation(nop, relationPart ?: "AND")
            }
            // endregion
            // region Album
            else if (opPart.startsWith("al:"))  {
                val albumSearch = opPart.removePrefix("al:").removeSurrounding("\"")
                val nop = albumSearch(albumSearch)
                addWithRelation(nop, relationPart ?: "AND")
            }
            // endregion
            // region Year
            else if (opPart.startsWith("y:")) {
                val yearSearch = opPart.removePrefix("y:").removeSurrounding("\"")
                val yearParts = yearSearch.split("-")

                val op = minMaxSearchInt(TrackTable.year, yearParts)
                addWithRelation(op, relationPart ?: "AND")
            }
            // endregion
            // region Duration
            else if (opPart.startsWith("d:")) {
                val durationSearch = opPart.removePrefix("d:").removeSurrounding("\"")
                val durationParts = durationSearch.split("-")

                val op = minMaxSearchLong(TrackTable.duration, durationParts)
                addWithRelation(op, relationPart ?: "AND")
            }
            // endregion
            // region Bitrate
            else if (opPart.startsWith("b:")) {
                val bitrateSearch = opPart.removePrefix("b:").removeSurrounding("\"")
                val birateParts = bitrateSearch.split("-")

                val op = minMaxSearchInt(TrackTable.bitrate, birateParts)
                addWithRelation(op, relationPart ?: "AND")
            }
            // endregion
            // region Explicit
            else if (opPart.startsWith("e:")) {
                val explicitSearch = opPart.removePrefix("e:").lowercase()
                if (explicitSearch in trueBooleans) {
                    val nop = (TrackTable.explicit eq true)
                    addWithRelation(nop, relationPart ?: "AND")
                }
                else if (explicitSearch in falseBooleans) {
                    val nop = (TrackTable.explicit eq false)
                    addWithRelation(nop, relationPart ?: "AND")
                }
            }
            // endregion
            // region Tags
            else if (opPart.startsWith("+") || opPart.startsWith("-"))  {
                val tagSearch = opPart.removePrefix("+").removePrefix("-").removeSurrounding("\"")
                val isInclude = opPart.startsWith("+")

                val tagFindQuery = TagTable.select(TagTable.id).where { TagTable.name.lowerCase() eq tagSearch.lowercase() }
                val connectionQuery = TrackTagConnection.select(TrackTagConnection.track).where { TrackTagConnection.tag inSubQuery tagFindQuery }

                val nop = if (isInclude) {
                    (TrackTable.id inSubQuery connectionQuery)
                } else {
                    (TrackTable.id notInSubQuery connectionQuery)
                }
                addWithRelation(nop, relationPart ?: "AND")
            }
            // endregion
            // region Other
            else {
                val search = opPart.removeSurrounding("\"")
                val nop = titleSearch(search) or artistSearch(search) or albumSearch(search)
                addWithRelation(nop, relationPart ?: "AND")
            }
            // endregion
        }

        return op ?: Op.TRUE
    }

    private fun minMaxSearchLong(col: Column<Long?>, parts: List<String>): Op<Boolean> {
        val numParts = parts.map { it.toLongOrNull() }
        return minMaxSearchGeneric(col, numParts)
    }

    private fun minMaxSearchInt(col: Column<Int?>, parts: List<String>): Op<Boolean> {
        val numParts = parts.map { it.toIntOrNull() }
        return minMaxSearchGeneric(col, numParts)
    }

    private fun <T>minMaxSearchGeneric(col: Column<T?>, parts: List<T?>): Op<Boolean> where T: Comparable<T>, T: Number {
        if (parts.size == 1 && parts[0] != null) {
            val v = parts[0]
            return (col eq v)
        }
        else if (parts.size == 2) {
            val min = parts[0]
            val max = parts[1]

            var op: Op<Boolean> = (col neq null)
            var added = false

            if (min != null) {
                op = op and (col greaterEq min)
                added = true
            }

            if (max != null) {
                op = op and (col lessEq max)
                added = true
            }

            if (added)
                return op
        }

        return Op.TRUE
    }

    private fun albumSearch(query: String): Op<Boolean> {
        return (TrackTable.albumId inSubQuery (
                AlbumTable.select(AlbumTable.id).where {
                    AlbumTable.title.lowerCase() like "%${query.lowercase()}%"
                }
            )
        )
    }

    private fun artistSearch(query: String): Op<Boolean> {
        return (TrackTable.id inSubQuery (
                TrackArtistConnection.select(TrackArtistConnection.track).where {
                    TrackArtistConnection.artist inSubQuery (
                            ArtistTable.select(ArtistTable.id).where {
                                ArtistTable.name.lowerCase() like "%${query.lowercase()}%"
                            }
                    )
                }
            )
        )
    }

    private fun titleSearch(query: String): Op<Boolean> {
        return (TrackTable.title.lowerCase() like "%${query.lowercase()}%")
    }
}