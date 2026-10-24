# Atelier Pad 3.0 — HTML arayüz, Kotlin motor

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

Akıcılık garantisi verilmez: hedef Android cihazda release profillemesi henüz yapılmadı. El yazısı çizim/OCR yoktur; kalem simgeleri hızlı metin notu oluşturur. Fotoğraf simgesi sistem seçicisini açar. iCloud entegrasyonu yoktur; durum bilgisi gerçek yerel kaydı ifade eder. Önceki sürümdeki URI tabanlı fotoğraflar veritabanında korunur; yeni HTML editörü yalnızca bu sürümde eklenmiş veri-URL fotoğraflarını gösterir.

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
