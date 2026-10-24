# Atelier Pad 3.1 — HTML arayüz, Kotlin motor

Nordic Atelier referansına göre hazırlanmış, tamamen yerel ve çevrimdışı Android uygulaması. **Compose kullanılmaz.** Arayüz HTML/CSS/JavaScript, veri ve Android işlemleri Kotlin'dir.

## Mimari

- `app/src/main/assets/`: bağımsız HTML/CSS/JS; APK'ya gömülü Manrope ve Newsreader fontları. CDN, Tailwind çalışma zamanı veya internet bağımlılığı yoktur.
- `MainActivity.kt`: donanım hızlandırmalı WebView, güvenli WebMessage köprüsü, görsel seçimi ve arka plan iş kuyruğu.
- `Library.kt`: Room/SQLite, not işlemleri, önceki sürümün SharedPreferences verilerinin bir kerelik aktarımı. İçe aktarma hatası kullanıcı verisini örnek içerikle değiştirmez.
- Köprü yalnızca `https://appassets.androidplatform.net` ana çerçevesinden çağrılabilir. Dosya erişimi, dış gezinme ve dış ağ istekleri kapalı; CSP etkin. Kullanıcı notları HTML olarak çalıştırılmaz.

## Özellikler

Arama (not içeriği dahil), klasörler, favoriler, sıralama, yeni not/klasör, üç renk kapak, metin düzenleme, 700 ms gecikmeli otomatik kayıt, fotoğraf ekleme, çöp kutusu, geri yükleme ve onaylı kalıcı silme. Masaüstü/tablet düzeni referansı takip eder; telefonda menü açılır ve kartlar uyarlanır.

## Performans tercihleri

- Room erişimi ve görsel çözme ana iş parçacığında yapılmaz.
- Not listesi tam içerik veya fotoğraf taşımaz; yalnızca açılan notun içeriği alınır.
- Arama 220 ms gecikmelidir; Kotlin tarafında yapılır. Çok büyük kitaplıklarda SQL tabanlı arama/sayfalama bir sonraki optimizasyondur; mevcut sürüm arama için kayıtları arka planda tarar.
- Kartlar 40'lık gruplarla oluşturulur, `content-visibility` kullanılır.
- Ağır blur ve sürekli animasyon yoktur; görseller en fazla yaklaşık 1600 px boyutuna örneklenir.
- Her not ayrı kaydedilir; bütün kitaplık her tuşta tekrar yazılmaz.

Akıcılık garantisi verilmez: hedef Android cihazda release profillemesi henüz yapılmadı. El yazısı çizimi vardır; OCR ve el yazısını metne çevirme yoktur. Fotoğraf simgesi sistem seçicisini açar. iCloud entegrasyonu yoktur; durum bilgisi gerçek yerel kaydı ifade eder. Önceki sürümdeki URI tabanlı fotoğraflar, erişim izni hâlâ geçerliyse ilk açılışta yeni görsel formatına aktarılır.

## APK

GitHub **Actions → Android APK → başarılı çalıştırma → Artifacts → Atelier-Pad-debug-APK** üzerinden ZIP'i indirin. ZIP içindeki `app-debug.apk` dosyasını Android cihazda açın. Debug APK test içindir; mağaza yayını için kişisel release imzası gerekir. Artifact 30 gün saklanır.

`main` veya çalışma dalına push APK derlemesini ve birim testlerini tetikler.

## Yerel derleme

Android Studio Meerkat+, JDK 17, Android SDK 35, minimum Android 8 / API 26 ve güncel Android System WebView gerekir.

```sh
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Font lisansları `licenses/`, tasarım referansı `docs/` içindedir. Emülatör ve fiziksel cihaz görsel karşılaştırması ayrıca yapılmalıdır.

## Arayüz mantık testleri

```sh
npm install --no-save --prefix .cache/ui-test jsdom@26
NODE_PATH="$PWD/.cache/ui-test/node_modules" node tools/ui-test.cjs
```

Testler: kart oluşturma, kullanıcı HTML içeriğini kaçışlama, filtreler, favoriler, editör, kayıt, çöp kutusu ve geri yükleme. Bunlar gerçek Android WebView ve cihaz performans testinin yerine geçmez.

## Yeni ekranlar: şablon stüdyosu ve çizim masası

- **Yeni → Yeni Not Defteri**: canlı önizleme; kareli, çizgili, düz, Cornell, noktalı ve haftalık plan; dört kağıt tonu, üç çizgi aralığı, dikey/yatay, GoodNotes oranı/A4 oranı ve kapak rengi. Seçimler Kotlin/Room'a kalıcı kaydedilir.
- **Deftere dokunma**: tam ekran çizim masası; basınca duyarlı kalem (donanım destekliyorsa), tükenmez/fırça, renk/kalınlık/opaklık, vurgulayıcı, silgi, gerçek düz çizgi, kementle seçip taşıma. T aracı mevcut metin/fotoğraf notlarını açar.
- Sayfa bazlı vektör çizimler, yıldız, yeni sayfa, önceki/sonraki sayfa, odak modu, %50–200 yakınlaştırma, temizleme, geri al/ileri al. Geçerli sayfanın son 25 işlemi için geri alma belleği tutulur; sayfa değiştirilince işlem geçmişi sıfırlanır, çizimler korunur.
- **PDF İçe Aktar** Android dosya seçicisini açar; dosya uygulama alanına kopyalanır. En fazla 50 MB / 200 sayfa. PdfRenderer yalnızca görünür sayfayı açar; PDF üzerine mürekkep eklenebilir. Kaynak PDF, defter kalıcı silinince temizlenir.
- **Dışa Aktar**: geçerli çizim sayfasını gerçek PNG veya raster PDF olarak Android dosya kaydetme seçicisiyle saklar. Çok sayfalı veya vektörel PDF dışa aktarımı yoktur; ayrı metin/fotoğraf paneli çizim çıktısına dahil değildir.
- Room v1 → v2 SQL migration mevcut notları silmeden `document` kolonunu ekler. Her defter en fazla 200 sayfa, çizim belgesi en fazla 4 milyon karakter olabilir.
- Örnek `urban` defterinin ilk sayfasında referansı yansıtan mimari demo çizimi vardır; kullanıcıların oluşturduğu defterler boş başlar.

Çizimler her kalem hareketinde Kotlin'e gönderilmez; JS kanvasında işlenir ve 700 ms bekleme sonrasında vektör belgesi arka planda kaydedilir. Liste sorgusu çizim/fotoğraf verilerini SQLite'dan almaz. Bitmap ekran görüntülerinden oluşan geri alma yığını kullanılmaz; yalnızca görünür sayfa için sınırlı boyutlu kanvas vardır. Aynı defterin tüm çizim verisi kayıt sırasında gönderilir; çok büyük belgeler için sayfa tabanlı kayıt gelecekteki optimizasyondur.

### Gerçek tarayıcı doğrulaması

```sh
npm install --no-save --prefix .cache/ui-test playwright-core@1.51.1 @sparticuz/chromium@133.0.0
NODE_PATH="$PWD/.cache/ui-test/node_modules" node tools/browser-test.cjs
```

Linux ortamında Chromium sistem kütüphaneleri gerekir. Test, gerçek Canvas çizimini, geri al/ileri almayı, sayfa ayrımını, kayıt-açılış döngüsünü, canlı önizlemeyi ve telefon genişliğini doğrular. Ekran görüntüleri `.cache/screenshots/` altında test çıktısıdır; Git'e eklenmez. Bu kontrol Android WebView, stylus gecikmesi veya fiziksel cihaz testinin yerine geçmez.
