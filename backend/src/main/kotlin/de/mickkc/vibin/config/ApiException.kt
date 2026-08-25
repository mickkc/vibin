package de.mickkc.vibin.config

import de.mickkc.vibin.dto.errors.ErrorDtoType

class ApiException(val errorType: ErrorDtoType, vararg val data: Pair<String, String>) : Exception(errorType.name)