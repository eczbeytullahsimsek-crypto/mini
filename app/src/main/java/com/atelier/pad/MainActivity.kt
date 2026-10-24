package com.atelier.pad

import android.os.Bundle
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

private val Cream = Color(0xFFFFF8F3)
private val Sidebar = Color(0xFFF9F2ED)
private val Container = Color(0xFFF3EDE8)
private val Green = Color(0xFF4B6151)
private val Sage = Color(0xFF637A69)
private val Ink = Color(0xFF1D1B18)
private val Muted = Color(0xFF737973)
private val Purple = Color(0xFF635B71)
private val Sand = Color(0xFF82725B)
private val Sans = FontFamily(Font(R.font.manrope))
private val Serif = FontFamily(Font(R.font.newsreader), Font(R.font.newsreader_italic, style = FontStyle.Italic))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Green, background = Cream, surface = Cream,
                    onSurface = Ink, secondary = Purple),
                typography = Typography(bodyLarge = TextStyle(fontFamily = Sans, fontSize = 15.sp),
                    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 13.sp),
                    labelLarge = TextStyle(fontFamily = Sans, fontSize = 14.sp))
            ) { Surface(color = Cream) { Atelier() } }
        }
    }
}

@Composable
private fun Atelier(library: Library = viewModel()) {
    var section by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var ascending by rememberSaveable { mutableStateOf(false) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderId by rememberSaveable { mutableStateOf<String?>(null) }
    var drawer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val b = Notebook(UUID.randomUUID().toString(), "Fotoğraf Notu", imageUri = uri.toString(), parentId = folderId)
            library.put(b); openId = b.id
        }
    }
    val openBook = library.books.find { it.id == openId }
    if (openBook != null) {
        Editor(openBook, onBack = { openId = null }, onSave = library::put,
            onDelete = { library.put(openBook.copy(deleted = true)); openId = null },
            onRemove = { library.remove(openBook); openId = null })
        return
    }
    BackHandler(folderId != null || drawer) { folderId = null; drawer = false }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val wide = maxWidth >= 1000.dp
        val tablet = maxWidth >= 700.dp
        val folder = library.books.find { it.id == folderId }
        val visible = queryLibrary(library.books, section, filter, search, onlyFavorites, folderId, ascending)
        Row(Modifier.fillMaxSize()) {
            if (tablet) SidePanel(section, { section = it; folderId = null }, Modifier.width(if (wide) 288.dp else 224.dp))
            Column(Modifier.weight(1f)) {
                Header(search, { search = it }, wide, tablet, { drawer = true }, { showFilters = true }, { showProfile = true })
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(horizontal = if (tablet) 32.dp else 20.dp)) {
                        if (tablet) {
                            Row(Modifier.fillMaxWidth().padding(bottom = 32.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                ShelfHeading(section, folder?.title, library.books.count { !it.deleted && !it.folder }, library.books.count { !it.deleted && it.folder })
                                if (wide) Filters(filter, { filter = it }, ascending, { ascending = !ascending })
                            }
                        } else {
                            Spacer(Modifier.height(20.dp))
                            ShelfHeading(section, folder?.title, library.books.count { !it.deleted && !it.folder }, library.books.count { !it.deleted && it.folder })
                            Spacer(Modifier.height(20.dp))
                        }
                        if (!wide) {
                            Filters(filter, { filter = it }, ascending, { ascending = !ascending })
                            Spacer(Modifier.height(24.dp))
                        }
                        if (folderId != null) {
                            TextButton(onClick = { folderId = null }) { Icon(Icons.Outlined.ArrowBack, null); Text("  Kitaplığa dön") }
                        }
                        if (visible.isEmpty()) {
                            Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.AutoStories, null, tint = Sage, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(if (search.isNotBlank()) "Sonuç bulunamadı" else "Henüz bir not yok", fontFamily = Serif, fontSize = 26.sp)
                                Text("Yeni bir defterle düşüncelerinize yer açın.", color = Muted, modifier = Modifier.padding(top = 8.dp))
                                TextButton(onClick = { showCreate = true }) { Text("Yeni oluştur") }
                            }
                        } else LazyVerticalGrid(
                            columns = if (wide) GridCells.Fixed(4) else GridCells.Adaptive(205.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
                            contentPadding = PaddingValues(bottom = 112.dp)
                        ) {
                            items(visible, key = { it.id }) { book ->
                                BookCard(book, library.books.count { it.parentId == book.id && !it.deleted },
                                    onFavorite = { library.put(book.copy(favorite = !book.favorite)) },
                                    onOpen = { if (book.folder && !book.deleted) folderId = book.id else openId = book.id })
                            }
                        }
                    }
                    CreationBar(Modifier.align(Alignment.BottomEnd).padding(end = if (tablet) 32.dp else 20.dp, bottom = 30.dp),
                        onNew = { showCreate = true }, onPhoto = { imagePicker.launch(arrayOf("image/*")) }, onQuick = {
                            val b = Notebook(UUID.randomUUID().toString(), "Hızlı Not", parentId = folderId)
                            library.put(b); openId = b.id
                        })
                }
            }
        }
        if (drawer && !tablet) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f)).clickable { drawer = false })
            Surface(shadowElevation = 12.dp) {
                SidePanel(section, { section = it; folderId = null; drawer = false }, Modifier.width(288.dp))
            }
        }
    }
    if (showFilters) AlertDialog(onDismissRequest = { showFilters = false },
        title = { Text("Kitaplık filtreleri", fontFamily = Serif) }, text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(onlyFavorites, { onlyFavorites = it }); Text("Yalnızca favorileri göster")
            }
        }, confirmButton = { TextButton(onClick = { showFilters = false }) { Text("Uygula") } },
        dismissButton = { TextButton(onClick = { onlyFavorites = false; filter = 0; search = ""; showFilters = false }) { Text("Sıfırla") } })
    if (showProfile) AlertDialog(onDismissRequest = { showProfile = false },
        icon = { Icon(Icons.Outlined.PersonOutline, null) }, title = { Text("Atelier Studio", fontFamily = Serif) },
        text = { Text("Kişisel kitaplığınız\n\nNotlarınız bu cihazda saklanır. iCloud veya başka bir bulut hizmeti bağlı değildir.\n\nv2.4 Atelier Pad") },
        confirmButton = { TextButton(onClick = { showProfile = false }) { Text("Tamam") } })
    if (showCreate) CreateDialog(onDismiss = { showCreate = false }, onCreate = { title, isFolder, color ->
        val b = Notebook(UUID.randomUUID().toString(), title, folder = isFolder, color = color, parentId = folderId)
        library.put(b); showCreate = false
        if (!isFolder) openId = b.id
    })
}

@Composable
private fun SidePanel(section: Int, onSection: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().background(Sidebar).padding(horizontal = 16.dp, vertical = 24.dp)) {
        Row(Modifier.padding(12.dp, 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Green, shape = RoundedCornerShape(12.dp), shadowElevation = 3.dp) {
                Icon(Icons.Outlined.EditNote, "Atelier", tint = Color.White, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Column {
                Text("Atelier", fontFamily = Serif, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text("NORDİC STUDIO", fontSize = 11.sp, letterSpacing = .7.sp)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 24.dp), color = Color(0xFFEDE7E2))
        listOf("Klasörler" to Icons.Outlined.FolderOpen, "Favoriler" to Icons.Outlined.BookmarkBorder, "Çöp Kutusu" to Icons.Outlined.DeleteOutline)
            .forEachIndexed { index, (label, icon) ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (section == index) Sage else Color.Transparent).clickable { onSection(index) }.padding(14.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(icon, null, tint = if (section == index) Color.White else Green, modifier = Modifier.size(22.dp))
                    Text(label, color = if (section == index) Color.White else Ink, fontSize = 14.sp)
                }
            }
        Spacer(Modifier.weight(1f))
        Row(Modifier.padding(horizontal = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Container).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CloudDone, null, tint = Green, modifier = Modifier.size(20.dp))
            Column { Text("Cihazda kaydedildi", fontSize = 11.sp); Text("Çevrimdışı kitaplık", fontSize = 11.sp, color = Muted) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("v2.4 Atelier Pad", fontSize = 11.sp)
            Icon(Icons.Outlined.Draw, null, modifier = Modifier.size(18.dp), tint = Green)
        }
    }
}

@Composable
private fun Header(search: String, onSearch: (String) -> Unit, wide: Boolean, tablet: Boolean, onMenu: () -> Unit, onFilter: () -> Unit, onProfile: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Cream)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = if (tablet) 32.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (wide) 20.dp else 8.dp)) {
            if (!tablet) IconButton(onClick = onMenu) { Icon(Icons.Outlined.Menu, "Menüyü aç") }
            Text("Not Defteri", fontFamily = Serif, fontSize = 24.sp, maxLines = 1)
            if (wide) {
                Box(Modifier.width(1.dp).height(16.dp).background(Color(0xFFE7E1DC)))
                Text("Atelier Koleksiyonu", fontSize = 12.sp, modifier = Modifier.clip(CircleShape).background(Container).padding(horizontal = 10.dp, vertical = 3.dp))
            }
            if (tablet) SearchField(search, onSearch, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            Surface(onClick = onFilter, color = Container, shape = RoundedCornerShape(9.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Tune, "Filtrele", modifier = Modifier.size(18.dp))
                    if (wide) Text("Filtrele", fontSize = 12.sp)
                }
            }
            Surface(onClick = onProfile, shape = CircleShape, color = Green) {
                Icon(Icons.Outlined.PersonOutline, "Profil", tint = Color.White, modifier = Modifier.padding(8.dp).size(18.dp))
            }
        }
        if (!tablet) SearchField(search, onSearch, Modifier.fillMaxWidth().padding(horizontal = 20.dp))
    }
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, modifier: Modifier) {
    Row(modifier.height(36.dp).clip(RoundedCornerShape(12.dp)).background(Container).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Outlined.Search, null, tint = Muted, modifier = Modifier.size(18.dp))
        BasicTextField(value, onValue, singleLine = true, textStyle = TextStyle(fontFamily = Sans, fontSize = 13.sp, color = Ink),
            modifier = Modifier.weight(1f), decorationBox = { inner ->
                Box { if (value.isEmpty()) Text("Notlarda, etiketlerde ve el yazısında ara...", fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis); inner() }
            })
        if (value.isNotEmpty()) Icon(Icons.Outlined.Close, "Aramayı temizle", tint = Muted, modifier = Modifier.size(18.dp).clickable { onValue("") })
    }
}

@Composable
private fun ShelfHeading(section: Int, folder: String?, books: Int, folders: Int) {
    Column {
        Text("NORDİC ATELİER · GOODNOTES SLATE", fontSize = 11.sp, letterSpacing = 1.sp, color = Color(0xFF424843))
        Text(folder ?: listOf("Kişisel Kitaplık", "Favoriler", "Çöp Kutusu")[section], fontFamily = Serif, fontSize = 32.sp, lineHeight = 40.sp)
        Text(if (section == 0 && folder == null) "$books defter, $folders akıllı klasör" else if (section == 2) "Silinen notlarınızı geri yükleyin" else "Düşünceleriniz için özenli bir alan", fontSize = 13.sp, color = Color(0xFF424843))
    }
}

@Composable
private fun Filters(selected: Int, onSelect: (Int) -> Unit, ascending: Boolean, onSort: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Sidebar).padding(4.dp)) {
            listOf("Tümü", "Klasörler", "Defterler").forEachIndexed { i, label ->
                Surface(onClick = { onSelect(i) }, color = if (i == selected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(8.dp), shadowElevation = if (i == selected) 2.dp else 0.dp) {
                    Text(label, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }
        }
        Surface(onClick = onSort, color = Sidebar, shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.SwapVert, "Sıralama", modifier = Modifier.size(17.dp))
                BoxWithConstraints { if (maxWidth > 115.dp) Text(if (ascending) "Ada göre" else "Son Değiştirilme", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun BookCard(book: Notebook, children: Int, onFavorite: () -> Unit, onOpen: () -> Unit) {
    Surface(onClick = onOpen, shape = RoundedCornerShape(18.dp), color = Color.White,
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth().height(320.dp)) {
        if (book.folder) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F0FA)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FolderCopy, null, tint = Purple, modifier = Modifier.size(28.dp))
                    }
                    Favorite(book.favorite, onFavorite, false)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Akademik", fontSize = 11.sp, color = Purple, modifier = Modifier.clip(CircleShape).background(Color(0xFFF7F3FB)).padding(horizontal = 8.dp, vertical = 2.dp))
                        Text(if (book.id == "academic" && children == 0) "6 Not Defteri" else "$children Not Defteri", fontSize = 10.sp)
                    }
                    Text(book.title, fontFamily = Serif, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Text(book.description, fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                }
                Column {
                    HorizontalDivider(color = Container)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(book.date, fontSize = 11.sp, color = Muted)
                        Icon(Icons.Outlined.ArrowForward, null, tint = Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            Column {
                Box(Modifier.fillMaxWidth().height(208.dp).background(listOf(Sage, Sand, Purple)[book.color.coerceIn(0, 2)])) {
                    Box(Modifier.width(16.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .12f), Color.Transparent))))
                    Box(Modifier.padding(start = 14.dp).width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = .18f)))
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Text(book.tag, fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = .2f)).padding(horizontal = 8.dp, vertical = 2.dp))
                            Favorite(book.favorite, onFavorite, true)
                        }
                        Surface(color = Color(0xFFF3EDE6), shape = RoundedCornerShape(14.dp), shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(book.coverTitle, fontFamily = Serif, fontSize = 22.sp, lineHeight = 28.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(book.coverSubtitle, fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 12.sp, lineHeight = 15.sp,
                                    textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(book.paper, color = Color.White.copy(alpha = .8f), fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text("${book.pages} Sayfa", color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().weight(1f).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(book.title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(book.description, fontSize = 13.sp, color = Color(0xFF424843), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                    Column {
                        HorizontalDivider(color = Container)
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(book.date, fontSize = 10.sp, color = Muted)
                            Text(if (book.deleted) "Silindi" else if (book.color == 1) "Çevrimdışı" else "• Cihazda", fontSize = 10.sp, color = Green)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Favorite(selected: Boolean, onClick: () -> Unit, cover: Boolean) {
    Surface(onClick = onClick, shape = CircleShape, color = if (cover) Color.White.copy(alpha = .25f) else Color.Transparent) {
        Icon(if (selected) Icons.Outlined.Star else Icons.Outlined.StarBorder,
            if (selected) "Favorilerden çıkar" else "Favorilere ekle", tint = if (cover) Color.White else Green,
            modifier = Modifier.padding(6.dp).size(17.dp))
    }
}

@Composable
private fun CreationBar(modifier: Modifier, onNew: () -> Unit, onPhoto: () -> Unit, onQuick: () -> Unit) {
    Surface(modifier, shape = CircleShape, color = Color.White, shadowElevation = 8.dp) {
        Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQuick) { Icon(Icons.Outlined.Draw, "Hızlı not") }
            IconButton(onClick = onQuick) { Icon(Icons.Outlined.Edit, "Yazılı not") }
            IconButton(onClick = onPhoto) { Icon(Icons.Outlined.PhotoCamera, "Fotoğraf ekle") }
            Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(24.dp).background(Container))
            Surface(onClick = onNew, shape = CircleShape, color = Green, shadowElevation = 3.dp) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp)); Text("Yeni", fontSize = 14.sp)
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CreateDialog(onDismiss: () -> Unit, onCreate: (String, Boolean, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var folder by rememberSaveable { mutableStateOf(false) }
    var color by rememberSaveable { mutableStateOf(0) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Yeni bir başlangıç", fontFamily = Serif, fontSize = 28.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!folder, { folder = false }, { Text("Defter") })
                    FilterChip(folder, { folder = true }, { Text("Klasör") })
                }
                OutlinedTextField(name, { name = it }, label = { Text(if (folder) "Klasör adı" else "Defter adı") }, singleLine = true)
                if (!folder) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Sage, Sand, Purple).forEachIndexed { i, c ->
                        Box(Modifier.size(44.dp).clip(CircleShape).background(c).border(if (i == color) 3.dp else 0.dp, Ink, CircleShape)
                            .clickable { color = i }, contentAlignment = Alignment.Center) {
                            if (i == color) Icon(Icons.Outlined.Check, "Seçili kapak", tint = Color.White)
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { onCreate(name.trim(), folder, color) }, enabled = name.isNotBlank()) { Text("Oluştur") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } })
}

@Composable
private fun Editor(book: Notebook, onBack: () -> Unit, onSave: (Notebook) -> Unit, onDelete: () -> Unit, onRemove: () -> Unit) {
    var title by rememberSaveable(book.id) { mutableStateOf(book.title) }
    var content by rememberSaveable(book.id) { mutableStateOf(book.content) }
    var confirmDelete by remember { mutableStateOf(false) }
    fun save() {
        if (!book.deleted && (title != book.title || content != book.content)) {
            onSave(book.copy(title = title.ifBlank { "İsimsiz Defter" }, content = content,
                description = content.lineSequence().firstOrNull { it.isNotBlank() }?.take(100) ?: book.description,
                date = "Bugün", modified = System.currentTimeMillis()))
        }
    }
    BackHandler { save(); onBack() }
    Column(Modifier.fillMaxSize().safeDrawingPadding().background(Cream)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { save(); onBack() }) { Icon(Icons.Outlined.ArrowBack, "Kitaplığa dön") }
            Text(if (book.deleted) "Çöp Kutusu" else "Atelier · Not Defteri", fontFamily = Serif, fontSize = 22.sp, modifier = Modifier.weight(1f))
            if (book.deleted) {
                TextButton(onClick = { onSave(book.copy(deleted = false)); onBack() }) { Text("Geri yükle") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.DeleteForever, "Kalıcı olarak sil") }
            } else {
                TextButton(onClick = { save(); onBack() }) { Text("Kaydet") }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Çöp kutusuna taşı") }
            }
        }
        HorizontalDivider(color = Container)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp)) {
            BasicTextField(title, { title = it }, readOnly = book.deleted,
                textStyle = TextStyle(fontFamily = Serif, fontSize = 34.sp, color = Ink), modifier = Modifier.fillMaxWidth())
            Text("${book.paper}  ·  ${book.pages} sayfa", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 16.dp))
            if (book.imageUri.isNotEmpty()) NoteImage(book.imageUri)
            BasicTextField(content, { content = it }, readOnly = book.deleted,
                textStyle = TextStyle(fontFamily = Sans, fontSize = 17.sp, lineHeight = 30.sp, color = Ink),
                modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp), decorationBox = { inner ->
                    Box { if (content.isEmpty()) Text("Bir düşünceyle başlayın…", color = Muted, fontSize = 17.sp); inner() }
                })
        }
        Text("Notlar cihazınızda saklanır · Çıkarken kaydedilir", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(20.dp))
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Kalıcı olarak silinsin mi?") },
        text = { Text("Bu işlem geri alınamaz.") },
        confirmButton = { TextButton(onClick = onRemove) { Text("Kalıcı olarak sil", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Vazgeç") } })
}

@Composable
private fun NoteImage(uri: String) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, uri) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it, null, options) }
                val sample = generateSequence(1) { it * 2 }.first { options.outWidth / it <= 1600 && options.outHeight / it <= 1600 }
                context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }.getOrNull()
        }
    }
    bitmap?.let { Image(it.asImageBitmap(), "Nota eklenen fotoğraf", modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(bottom = 20.dp)) }
}
