package com.atelier.pad

import android.content.Context
import androidx.room.*
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey val id: String,
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
    val imageUri: String = "",
    @ColumnInfo(defaultValue = "''") val document: String = ""
)


@Dao
interface NotebookDao {
    @Query("SELECT id, title, description, coverTitle, coverSubtitle, tag, paper, pages, color, favorite, folder, deleted, content, modified, date, parentId, '' AS imageUri, '' AS document FROM notebooks ORDER BY modified DESC") fun all(): List<Notebook>
    @Query("SELECT * FROM notebooks WHERE id = :id") fun get(id: String): Notebook?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun put(book: Notebook)
    @Query("DELETE FROM notebooks WHERE id = :id") fun remove(id: String)
}

@Database(entities = [Notebook::class], version = 2, exportSchema = true)
abstract class AtelierDatabase : RoomDatabase() { abstract fun notebooks(): NotebookDao }

/** All calls are made on the activity's single background executor. */
class Library(context: Context) {
    private val db = Room.databaseBuilder(context.applicationContext, AtelierDatabase::class.java, "atelier.db").addMigrations(object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notebooks ADD COLUMN document TEXT NOT NULL DEFAULT ''")
        }
    }).build()
    val dao = db.notebooks()
    init {
        val prefs = context.getSharedPreferences("atelier_library", 0)
        if (!prefs.getBoolean("room_imported", false)) {
            val old = prefs.getString("books", null)
            // A failed import must not silently overwrite existing user data.
            val initial = if (old == null) samples() else JSONArray(old).let { a ->
                (0 until a.length()).map { notebookFromJson(a.getJSONObject(it)) }
            }
            db.runInTransaction { initial.forEach { if (dao.get(it.id) == null) dao.put(it) } }
            prefs.edit().putBoolean("room_imported", true).commit()
        }
    }
    fun close() = db.close()
}

fun notebookFromJson(o: JSONObject) = Notebook(
    id = o.getString("id"), title = o.getString("title"), description = o.optString("description"),
    coverTitle = o.optString("coverTitle", o.getString("title")), coverSubtitle = o.optString("coverSubtitle", "Kişisel notlar"),
    tag = o.optString("tag", "YENİ DEFTER"), paper = o.optString("paper", "Noktalı · Fildişi"), pages = o.optInt("pages", 1),
    color = o.optInt("color"), favorite = o.optBoolean("favorite"), folder = o.optBoolean("folder"),
    deleted = o.optBoolean("deleted"), content = o.optString("content"), modified = o.optLong("modified", System.currentTimeMillis()),
    date = o.optString("date", "Bugün"), parentId = o.optString("parentId").takeIf { it.isNotEmpty() }, imageUri = o.optString("imageUri")
)

fun Notebook.json(full: Boolean = true) = JSONObject().apply {
    put("id", id); put("title", title); put("description", description); put("coverTitle", coverTitle)
    put("coverSubtitle", coverSubtitle); put("tag", tag); put("paper", paper); put("pages", pages)
    put("color", color); put("favorite", favorite); put("folder", folder); put("deleted", deleted)
    put("modified", modified); put("date", date); put("parentId", parentId ?: "")
    // List responses never move full note bodies or photos across the WebView bridge.
    if (full) { put("content", content); put("imageUri", imageUri); put("document", document) }
}

fun samples() = listOf(
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
