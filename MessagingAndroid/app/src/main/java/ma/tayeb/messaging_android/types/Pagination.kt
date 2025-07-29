package ma.tayeb.messaging_android.types

data class Sort(
    val unsorted: Boolean,
    val sorted: Boolean,
    val empty: Boolean
)

data class Pageable(
    val sort: Sort,
    val pageNumber: Int,
    val pageSize: Int,
    val offset: Int,
    val paged: Boolean,
    val unpaged: Boolean
)

data class PaginatedResponse<T>(
    val content: List<T>,
    val pageable: Pageable,
    val totalPages: Int,
    val totalElements: Long,
    val last: Boolean,
    val size: Int,
    val number: Int,
    val sort: Sort,
    val numberOfElements: Int,
    val first: Boolean,
    val empty: Boolean
)
