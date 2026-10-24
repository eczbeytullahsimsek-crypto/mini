package com.atelier.pad

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
            val b = Notebook(UUID.randomUUID().toString(), title, folder = arg.optBoolean("folder"), color = arg.optInt("color").coerceIn(0, 2), parentId = arg.optString("parentId").takeIf { it.isNotEmpty() })
            dao.put(b); return b.json()
        }
        if (action == "photo") { runOnUiThread { photo.launch(arrayOf("image/*")) }; return true }
        val book = dao.get(arg.getString("id")) ?: error("Not bulunamadı")
        when (action) {
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
                dao.put(book.copy(title = arg.optString("title").trim().take(200).ifEmpty { "İsimsiz Defter" }, content = text,
                    description = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(100) ?: book.description,
                    modified = System.currentTimeMillis(), date = "Bugün"))
            }
            "favorite" -> dao.put(book.copy(favorite = !book.favorite))
            "trash" -> dao.put(book.copy(deleted = true))
            "restore" -> dao.put(book.copy(deleted = false))
            "remove" -> { require(book.deleted); dao.remove(book.id) }
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
