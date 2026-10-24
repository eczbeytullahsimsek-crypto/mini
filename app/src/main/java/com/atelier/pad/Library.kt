package com.atelier.pad

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject

data class Notebook(
    val id: String,
    val title: String,
    val description: String = "",
    val coverTitle: String = title,
    val coverSubtitle: String = "Kişisel notlar",
    val tag: String = "YENİ DEFTER",
    val paper: String = "Noktalı · Fildişi",
    val pages: Int = 1,
    val color: Int = 0,
    val favorite: Boolean = false,
    val folder: Boolean = false,
    val deleted: Boolean = false,
    val content: String = "",
    val modified: Long = System.currentTimeMillis(),
    val date: String = "Bugün",
    val parentId: String? = null,
    val imageUri: String = ""
)

class Library(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("atelier_library", 0)
    var books by mutableStateOf(load())
        private set

    private fun load(): List<Notebook> {
        val saved = preferences.getString("books", null) ?: return samples()
        return runCatching {
            val array = JSONArray(saved)
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                Notebook(o.getString("id"), o.getString("title"), o.optString("description"),
                    o.getString("coverTitle"), o.getString("coverSubtitle"), o.getString("tag"),
                    o.getString("paper"), o.getInt("pages"), o.getInt("color"),
                    o.getBoolean("favorite"), o.getBoolean("folder"), o.getBoolean("deleted"),
                    o.optString("content"), o.getLong("modified"), o.getString("date"),
                    o.optString("parentId").takeIf { it.isNotEmpty() }, o.optString("imageUri"))
            }
        }.getOrElse { samples() }
    }

    fun put(book: Notebook) {
        books = if (books.any { it.id == book.id }) books.map { if (it.id == book.id) book else it }
            else books + book
        persist()
    }
    fun remove(book: Notebook) { books = books.filterNot { it.id == book.id }; persist() }
    private fun persist() {
        val array = JSONArray()
        books.forEach { b -> array.put(JSONObject().apply {
            put("id", b.id); put("title", b.title); put("description", b.description)
            put("coverTitle", b.coverTitle); put("coverSubtitle", b.coverSubtitle)
            put("tag", b.tag); put("paper", b.paper); put("pages", b.pages); put("color", b.color)
            put("favorite", b.favorite); put("folder", b.folder); put("deleted", b.deleted)
            put("content", b.content); put("modified", b.modified); put("date", b.date)
            put("parentId", b.parentId ?: ""); put("imageUri", b.imageUri)
        }) }
        preferences.edit().putString("books", array.toString()).apply()
    }
}

private fun samples() = listOf(
    Notebook("academic", "Ders Notları & Tez", "Mimari tipolojiler, İskandinav formları ve tasarım eskizleri",
        favorite = true, folder = true, date = "Dün güncellendi", modified = 4),
    Notebook("urban", "İskandinav Strüktür Notları", "Ahşap birleşim detayları, kentsel eskizler",
        "Kentsel\nEskizler", "Helsinki & Stockholm\nSerisi", "CİLT I", "Noktalı · Fildişi", 48,
        favorite = true, date = "Bugün 14:20", modified = 3),
    Notebook("journal", "Günlük Yazılar ve Tipografi", "Sabah serbest yazımları, kitap notları",
        "Atölye\nGünlüğü", "2024 Güz Notları", "REFLEKSİYON", "Çizgili · Sıcak Kum", 112,
        color = 1, date = "18 Eki 2024", modified = 2),
    Notebook("cornell", "Sistem Tasarımı Notları", "Kullanıcı arayüzü prensipleri, bilişsel araştırma",
        "Cornell\nAnalizleri", "Bilişsel & UX Araştırma", "ÇALIŞMA METODU", "Cornell · Doğal Beyaz", 32,
        color = 2, favorite = true, date = "12 Eki 2024", modified = 1)
)
