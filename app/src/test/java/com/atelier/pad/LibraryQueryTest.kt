package com.atelier.pad

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryQueryTest {
    private val books = listOf(
        Notebook("1", "Alfa", favorite = true, modified = 1),
        Notebook("2", "Beta", folder = true, modified = 2),
        Notebook("3", "Silinen", deleted = true, favorite = true, modified = 3),
        Notebook("4", "Çizim", content = "Ahşap birleşim detayları", parentId = "2", modified = 4)
    )
    @Test fun deletedBooksOnlyAppearInTrash() {
        assertEquals(listOf("4", "2", "1"), queryLibrary(books).map { it.id })
        assertEquals(listOf("3"), queryLibrary(books, section = 2).map { it.id })
    }
    @Test fun favoritesExcludeDeletedBooks() {
        assertEquals(listOf("1"), queryLibrary(books, section = 1).map { it.id })
    }
    @Test fun folderAndNotebookFiltersWork() {
        assertEquals(listOf("2"), queryLibrary(books, filter = 1).map { it.id })
        assertEquals(listOf("4", "1"), queryLibrary(books, filter = 2).map { it.id })
    }
    @Test fun searchesNoteContentsIgnoringCaseAndOuterWhitespace() {
        assertEquals(listOf("4"), queryLibrary(books, search = " AHŞAP ").map { it.id })
    }
    @Test fun foldersContainOnlyTheirChildren() {
        assertEquals(listOf("4"), queryLibrary(books, folderId = "2").map { it.id })
    }
    @Test fun alphabeticalOrderAndCombinedFiltersWork() {
        assertEquals(listOf("1", "2", "4"), queryLibrary(books, ascending = true).map { it.id })
        assertEquals(emptyList<Notebook>(), queryLibrary(books, filter = 1, onlyFavorites = true))
    }
}
