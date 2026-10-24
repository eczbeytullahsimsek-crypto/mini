package com.atelier.pad

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfDocument
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import java.io.File
import android.util.Base64
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.webkit.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private val worker = Executors.newSingleThreadExecutor()
    private var library: Library? = null
    private val origin = "https://appassets.androidplatform.net"
    private val photo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            worker.execute {
                runCatching {
                    val b = Notebook(UUID.randomUUID().toString(), "Fotoğraf Notu", imageUri = encodeImage(uri))
                    repository().dao.put(b)
                    runOnUiThread { web.evaluateJavascript("window.photoAdded(${JSONObject.quote(b.id)})", null) }
                }.onFailure { runOnUiThread { web.evaluateJavascript("window.showError('Fotoğraf eklenemedi')", null) } }
            }
        }
    }
    private var exportBytes: ByteArray? = null
    private val exportPng = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri -> writeExport(uri) }
    private val exportPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> writeExport(uri) }
    private fun notifyUi(message: String) {
        runOnUiThread { if (!isDestroyed) web.evaluateJavascript("window.showError(${JSONObject.quote(message)})", null) }
    }
    private fun writeExport(uri: Uri?) {
        val bytes = exportBytes; exportBytes = null
        if (uri != null && bytes != null) worker.execute {
            runCatching { contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Dosya açılamadı") }
                .onSuccess { notifyUi("Dosya kaydedildi") }.onFailure { notifyUi("Dosya kaydedilemedi") }
        }
    }
    private val importPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) worker.execute {
            val id = UUID.randomUUID().toString()
            val file = File(filesDir, "$id.pdf")
            runCatching {
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192); var total = 0L
                        while (true) { val count = input.read(buffer); if (count < 0) break
                            total += count; require(total <= 50_000_000) { "PDF en fazla 50 MB olabilir" }; output.write(buffer, 0, count) }
                    }
                } ?: error("PDF açılamadı")
                val count = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { it.pageCount }
                require(count in 1..200) { "PDF en fazla 200 sayfa olabilir" }
                val pages = JSONArray()
                repeat(count) { pages.put(JSONObject().put("strokes", JSONArray()).put("pdfPage", it).put("starred", false)) }
                val doc = JSONObject().put("version", 1).put("template", "blank").put("paperColor", "white")
                    .put("spacing", 20).put("orientation", "portrait").put("size", "a4").put("pages", pages)
                repository().dao.put(Notebook(id, "İçe Aktarılan PDF", pages = count, document = doc.toString(), tag = "PDF"))
                runOnUiThread { if (!isDestroyed) web.evaluateJavascript("window.photoAdded(${JSONObject.quote(id)})", null) }
            }.onFailure { file.delete(); notifyUi(it.message ?: "PDF içe aktarılamadı") }
        }
    }
    private fun pdfPage(id: String, index: Int): String {
        require(id.matches(Regex("[a-zA-Z0-9-]+")))
        return PdfRenderer(ParcelFileDescriptor.open(File(filesDir, "$id.pdf"), ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            renderer.openPage(index).use { page ->
                val scale = 1400f / maxOf(page.width, page.height)
                val bitmap = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1), (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE); page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val bytes = ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes); bitmap.recycle()
                "data:image/png;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
            }
        }
    }
    private fun encodeImage(uri: Uri): String {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        require(opts.outWidth > 0) { "Görsel okunamadı" }
        var sample = 1
        while (opts.outWidth / sample > 1600 || opts.outHeight / sample > 1600) sample *= 2
        val bitmap = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Görsel okunamadı")
        val bytes = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bytes)
        bitmap.recycle()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    }
    private fun repository(): Library = library ?: Library(this).also { library = it }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        ViewCompat.setOnApplyWindowInsetsListener(web) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportZoom(false)
        }
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url) ?: WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), "".byteInputStream())
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(web, "Atelier", setOf(origin)) { _, message, source, mainFrame, reply ->
                if (!mainFrame || source.toString().trimEnd('/') != origin) return@addWebMessageListener
                val request = runCatching { JSONObject(message.data ?: "") }.getOrNull() ?: return@addWebMessageListener
                val id = request.optInt("requestId")
                worker.execute {
                    val result = JSONObject().put("requestId", id)
                    try { result.put("data", handle(request)) }
                    catch (e: Exception) { result.put("error", e.message ?: "İşlem tamamlanamadı") }
                    runOnUiThread { if (!isDestroyed) reply.postMessage(result.toString()) }
                }
            }
            web.loadUrl("$origin/assets/index.html")
        } else {
            web.loadData("<h2>Android System WebView uygulamasını güncelleyin.</h2>", "text/html", "UTF-8")
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("window.goBack ? window.goBack() : false") { result ->
                    if (result != "true") finish()
                }
            }
        })
    }
    private fun handle(r: JSONObject): Any {
        val dao = repository().dao
        val arg = r.optJSONObject("args") ?: JSONObject()
        val action = r.getString("action")
        if (action == "list") {
            val query = arg.optString("search").trim()
            return JSONArray(dao.all().filter { query.isEmpty() || listOf(it.title, it.description, it.content, it.tag, it.coverTitle).any { text -> text.contains(query, true) } }.map { it.json(false) })
        }
        if (action == "create") {
            val title = arg.getString("title").trim().take(200)
            require(title.isNotEmpty()) { "Bir ad yazın" }
            val doc = arg.optString("document")
            require(doc.length <= 4_000_000)
            if (doc.isNotEmpty()) JSONObject(doc)
            val b = Notebook(UUID.randomUUID().toString(), title, document = doc, paper = arg.optString("paper", "Noktalı · Fildişi"), folder = arg.optBoolean("folder"), color = arg.optInt("color").coerceIn(0, 2), parentId = arg.optString("parentId").takeIf { it.isNotEmpty() })
            dao.put(b); return b.json()
        }
        if (action == "photo") { runOnUiThread { photo.launch(arrayOf("image/*")) }; return true }
        if (action == "importPdf") { runOnUiThread { importPdf.launch(arrayOf("application/pdf")) }; return true }
        if (action == "export") {
            require(exportBytes == null) { "Önce açık dosya kaydetme işlemini tamamlayın" }
            val data = arg.getString("image"); require(data.startsWith("data:image/png;base64,") && data.length < 16_000_000)
            val png = Base64.decode(data.substringAfter(','), Base64.DEFAULT)
            val format = arg.optString("format")
            exportBytes = if (format == "pdf") {
                val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size) ?: error("Görsel açılamadı")
                val pdf = PdfDocument()
                val width = if (bitmap.width > bitmap.height) 842 else 595
                val height = if (bitmap.width > bitmap.height) 595 else 842
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
                page.canvas.drawBitmap(bitmap, null, Rect(0, 0, width, height), null)
                pdf.finishPage(page)
                val bytes = ByteArrayOutputStream(); pdf.writeTo(bytes); pdf.close(); bitmap.recycle(); bytes.toByteArray()
            } else png
            runOnUiThread { if (format == "pdf") exportPdf.launch("Atelier-sayfa.pdf") else exportPng.launch("Atelier-sayfa.png") }
            return true
        }
        val book = dao.get(arg.getString("id")) ?: error("Not bulunamadı")
        when (action) {
            "pdfPage" -> return pdfPage(book.id, arg.getInt("page"))
            "get" -> {
                // Import old Compose photo URIs lazily, outside the UI thread.
                if (book.imageUri.startsWith("content://")) {
                    val converted = book.copy(imageUri = encodeImage(Uri.parse(book.imageUri)))
                    dao.put(converted)
                    return converted.json()
                }
                return book.json()
            }
            "save" -> {
                require(!book.deleted)
                val text = arg.optString("content").take(1_000_000)
                val document = arg.optString("document", book.document)
                require(document.length <= 4_000_000) { "Çizim sınırına ulaşıldı; yeni bir defter oluşturun" }
                val pageCount = if (document.isNotEmpty()) JSONObject(document).getJSONArray("pages").length() else book.pages
                require(pageCount in 1..200)
                dao.put(book.copy(document = document, pages = pageCount, paper = arg.optString("paper", book.paper), title = arg.optString("title").trim().take(200).ifEmpty { "İsimsiz Defter" }, content = text,
                    description = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(100) ?: book.description,
                    modified = System.currentTimeMillis(), date = "Bugün"))
            }
            "favorite" -> dao.put(book.copy(favorite = !book.favorite))
            "trash" -> dao.put(book.copy(deleted = true))
            "restore" -> dao.put(book.copy(deleted = false))
            "remove" -> { require(book.deleted); dao.remove(book.id); if (book.id.matches(Regex("[a-zA-Z0-9-]+"))) File(filesDir, "${book.id}.pdf").delete() }
            else -> error("Desteklenmeyen işlem")
        }
        return true
    }
    override fun onPause() {
        web.evaluateJavascript("window.flushNote && window.flushNote()", null)
        super.onPause()
    }
    override fun onDestroy() {
        web.destroy()
        worker.execute { library?.close() }
        worker.shutdown()
        super.onDestroy()
    }
}
