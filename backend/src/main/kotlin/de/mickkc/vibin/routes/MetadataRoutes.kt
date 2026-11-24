package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.CreateMetadataDto
import de.mickkc.vibin.dto.CreatedMetadataDto
import de.mickkc.vibin.dto.MetadataProvidersDto
import de.mickkc.vibin.parsing.*
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.ArtistRepo
import de.mickkc.vibin.repos.TagRepo
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*

@OptIn(ExperimentalKtorApi::class)
fun Application.configureMetadataRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/metadata/providers") {
            val user = call.getUser() ?: unauthorized()
            call.respond(
                MetadataProvidersDto(
                    file = Parser.getAvailableFileProviders(user),
                    track = Parser.getAvailableTrackSearchProviders(user),
                    artist = Parser.getAvailableArtistSearchProviders(user),
                    album = Parser.getAvailableAlbumSearchProviders(user),
                    lyrics = Parser.getAvailableLyricsSearchProviders(user),
                    preParsers = Parser.getAvailablePreParsers(user)
                )
            )
        }.describe {
            operationId = "getMetadataProviders"
            summary = "Get all available metadata providers"
            tag("Metadata")
            authResponses {
                successResponse<MetadataProvidersDto>("Available metadata providers")
            }
        }

        get("/api/metadata/{type}") {
            val query = call.getStringParameter("q")
            val provider = call.getStringParameter("provider")
            val user = call.getUser() ?: unauthorized()

            if (query.isBlank()) {
                return@get call.respond(emptyList<Any>())
            }

            val results = when (call.parameters["type"]) {
                "track" -> Parser.searchTrack(query, provider, user)
                "artist" -> Parser.searchArtist(query, provider, user)
                "album" -> Parser.searchAlbum(query, provider, user)
                "lyrics" -> Parser.searchLyrics(query, provider, user)
                else -> invalidParameter("type", "track", "artist", "album", "lyrics")
            } ?: return@get call.respond(emptyList<Any>())
            call.respond(results)
        }.describe {
            operationId = "searchMetadata"
            summary = "Search for metadata"
            tag("Metadata")
            parameters {
                p("type", "track",
                    description = "What to search for: `track`, `artist`, `album` or `lyrics`.")
                q("q", "never gonna give you up", required = true,
                    description = "The search query")
                q("provider", "Spotify", required = true,
                    description = "The provider to search for")
            }
            responses {
                response(200) {
                    schema = JsonSchema(
                        items = ReferenceOr.Value(
                            JsonSchema(
                                oneOf = listOf(
                                    ReferenceOr.Value(jsonSchema<TrackInfoMetadata>()),
                                    ReferenceOr.Value(jsonSchema<ArtistMetadata>()),
                                    ReferenceOr.Value(jsonSchema<AlbumMetadata>()),
                                    ReferenceOr.Value(jsonSchema<LyricMetadata>()),
                                )
                            )
                        ),
                    )
                }
            }
        }

        postP("/api/metadata/create", PermissionType.MANAGE_TRACKS) {
            val request = call.receive<CreateMetadataDto>()
            request.validate()

            val user = call.getUser() ?: unauthorized()

            val artists = request.artistNames.map { ArtistRepo.getOrCreateArtist(it, user) }
            val tags = request.tagNames.map { TagRepo.getOrCreateTag(it) }
            val album = request.albumName?.let { AlbumRepo.getOrCreateAlbum(request.albumName, request.artistNames.firstOrNull(), user) }

            call.respond(CreatedMetadataDto(
                artists = ArtistRepo.toDto(artists),
                tags = TagRepo.toDto(tags),
                album = album?.let { AlbumRepo.toDto(it) }
            ))
        }.describe {
            operationId = "createMetadataEntities"
            summary = "Get or create artists, tags, and albums from names"
            tag("Metadata")
            requestBody {
                description = "Lists of artist names, tag names and an album name to get or create"
                schema = jsonSchema<CreateMetadataDto>()
            }
            responses {
                successResponse<CreatedMetadataDto>("The created entities")
                invalidParameterResponse("artistNames/tagNames/albumName",
                    "artistNames/tagNames/albumName contains a name that is longer than 255 characters")
            }
        }
    }
}
