package com.atelier.pad

/** Shared, deterministic query logic independent of the Compose screen. */
internal fun queryLibrary(
    books: List<Notebook>,
    section: Int = 0,
    filter: Int = 0,
    search: String = "",
    onlyFavorites: Boolean = false,
    folderId: String? = null,
    ascending: Boolean = false
): List<Notebook> = books.filter { book ->
    (if (section == 2) book.deleted else !book.deleted) &&
        (section != 1 || book.favorite) && (!onlyFavorites || book.favorite) &&
        (filter == 0 || (filter == 1 && book.folder) || (filter == 2 && !book.folder)) &&
        (folderId == null || book.parentId == folderId) &&
        (search.isBlank() || listOf(book.title, book.description, book.tag, book.content, book.coverTitle)
            .any { it.contains(search.trim(), ignoreCase = true) })
}.let { if (ascending) it.sortedBy { book -> book.title } else it.sortedByDescending { book -> book.modified } }
