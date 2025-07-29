export type Sort = {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
}

export type Pageable = {
    sort: Sort;
    pageNumber: number;
    pageSize: number;
    offset: number;
    paged: boolean;
    unpaged: boolean;
}

export type PaginatedResponse<T> = {
    content: T[];
    pageable: Pageable;
    totalPages: number;
    totalElements: number;
    last: boolean;
    size: number;
    number: number;
    sort: Sort;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}
