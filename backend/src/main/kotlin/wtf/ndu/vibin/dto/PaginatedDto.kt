package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedDto<T>(
    val items: List<T>,
    val total: Int,
    val pageSize: Int,
    val currentPage: Int,
    val hasNext: Boolean = total > currentPage * pageSize,
    val hasPrevious: Boolean = currentPage > 1
)