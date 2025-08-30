package wtf.ndu.vibin.search

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import wtf.ndu.vibin.db.*

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

            if (opPart.startsWith("(") && opPart.endsWith(")")) {
                val parts = split(opPart.removePrefix("(").removeSuffix(")"))
                val nop = buildQuery(parts)
                addWithRelation(nop, relationPart ?: "AND")
            }
            else if (opPart.startsWith("t:")) {
                val titleSearch = opPart.removePrefix("t:").removeSurrounding("\"")
                val nop = titleSearch(titleSearch)
                addWithRelation(nop, relationPart ?: "AND")
            }
            else if (opPart.startsWith("a:")) {
                val artistSearch = opPart.removePrefix("a:").removeSurrounding("\"")
                val nop = artistSearch(artistSearch)
                addWithRelation(nop, relationPart ?: "AND")
            }
            else if (opPart.startsWith("al:"))  {
                val albumSearch = opPart.removePrefix("al:").removeSurrounding("\"")
                val nop = albumSearch(albumSearch)
                addWithRelation(nop, relationPart ?: "AND")
            }
            else if (opPart.startsWith("y:")) {
                val yearSearch = opPart.removePrefix("y:")
                val yearParts = yearSearch.split("-")

                if (yearParts.size == 1) {
                    val year = yearParts[0].toIntOrNull()
                    if (year != null) {
                        val nop = (TrackTable.year eq year)
                        addWithRelation(nop, relationPart ?: "AND")
                    }
                }
                else if (yearParts.size == 2) {
                    val startYear = yearParts[0].toIntOrNull()
                    val endYear = yearParts[1].toIntOrNull()

                    var op: Op<Boolean>? = null

                    if (startYear != null) {
                        op = (TrackTable.year greaterEq startYear)
                    }

                    if (endYear != null) {
                        val nop = (TrackTable.year lessEq endYear)
                        op = if (op == null) { nop } else { op and nop }
                    }

                    if (op != null) {
                        addWithRelation(op, relationPart ?: "AND")
                    }
                }
            }
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
            else {
                val search = opPart.removeSurrounding("\"")
                val nop = titleSearch(search) or artistSearch(search) or albumSearch(search)
                addWithRelation(nop, relationPart ?: "AND")
            }
        }

        return op ?: Op.TRUE
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