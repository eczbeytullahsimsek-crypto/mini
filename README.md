# Atelier Pad — Kotlin / Jetpack Compose

Eklenen **Nordic Atelier** referansının Android için yerel uygulaması. HTML veya WebView kullanılmaz. Krem yüzeyler, üç renk kitap kapağı, cilt çizgileri, Newsreader / Manrope tipografisi, kitaplık kartları, kenar menüsü ve yüzen araç çubuğu Compose ile çizilir. Fontlar uygulamaya gömülüdür; internet gerekmez.

## Çalıştırma

1. Projeyi **Android Studio Meerkat veya daha yeni** sürümünde açın.
2. Gradle JDK olarak **JDK 17** seçin; **Android SDK 35** kurulu olsun.
3. Gradle senkronizasyonunu tamamlayın ve `app` yapılandırmasını çalıştırın.
4. Referans düzeni için yatay tablet / yeniden boyutlandırılabilir emülatör kullanın (ör. **1280 × 1024 dp**).

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

İlk senkronizasyonda Gradle, Google Maven ve Maven Central erişimi gerekir. `local.properties` dosyası cihazınıza özeldir; Android Studio oluşturur.

## Özellikler

- Geniş tablette solda kalıcı menü ve dört sütun; dar tablette iki/üç sütun, telefonda açılır menü ve uyarlanabilir kartlar.
- Türkçe kitaplık; başlık, açıklama, etiket, kapak ve not içeriğinde arama.
- Klasör / defter filtreleri, favoriler, alfabetik veya son değiştirilme sıralaması.
- Yeni defter ve klasör oluşturma, kapak rengi seçimi, klasör içinde yeni not.
- Metin notlarını düzenleme ve kaydetme; geri tuşuyla çıkarken otomatik kayıt.
- Fotoğraf seçicisinden görsel ekleme; notta görsel ve metni birlikte görüntüleme.
- Çöp kutusuna taşıma, geri yükleme ve onayla kalıcı silme.
- Yerel JSON / SharedPreferences saklama; uygulama yeniden açıldığında notlar korunur.
- Erişilebilir ikon açıklamaları; kaynak fotoğraflar için kalıcı okuma izni.

## Referanstan bilinçli farklar / sınırlar

- Referanstaki **“iCloud Eşitlendi”** alanı gerçek bir bulut entegrasyonu varmış izlenimi vermemek için **“Cihazda kaydedildi”** olarak gösterilir. Bulut senkronizasyonu yoktur.
- Kalem simgeleri hızlı **metin notu** oluşturur. El yazısı çizim, OCR ve gerçek kamera çekimi bu sürümün kapsamına dahil değildir; kamera simgesi sistemin görsel/dosya seçicisini açar.
- İlk ekrandaki akademik klasör referanstaki “6 Not Defteri” bilgisini taşır; bu altı defterin içeriği tasarım eklerinde bulunmadığından üretilmemiştir. Klasöre not eklendikçe gerçek yerel sayı gösterilir.
- Android sistem çubukları ve ekran yoğunluğu nedeniyle piksel düzeyinde aynılık cihazdan cihaza değişebilir; referansın masaüstü tasarımı dp birimleriyle tablet düzenine aktarılmıştır.
- Bu çalışma ortamında Java / Android SDK bulunmadığından Gradle derlemesi ve emülatör / ekran görüntüsü karşılaştırması çalıştırılamamıştır. APK teslimi değil, Android Studio kaynak projesidir.

## Dosyalar

- `app/src/main/java/com/atelier/pad/MainActivity.kt`: Compose ekranları, yerel görsel seçici, not editörü.
- `app/src/main/java/com/atelier/pad/Library.kt`: örnek kitaplık, veri modeli ve yerel saklama.
- `app/src/main/res/font/`: çevrimdışı Manrope ve Newsreader fontları.
- `licenses/`: SIL Open Font License metinleri.
- `docs/`: kullanıcının sağladığı tasarım rehberi ve ekran referansı.

Minimum Android: **8.0 / API 26**. Hedef: **API 35**. Kotlin **2.1.20**, AGP **8.9.1**, Gradle **8.11.1**.

## Android Studio olmadan APK (GitHub Actions)

`arena/d1f3a270-mini` dalına gönderildiğinde **Android APK** iş akışı testleri ve debug APK derlemesini otomatik çalıştırır.

1. GitHub deposunda **Actions → Android APK** ekranını açın.
2. Son başarılı çalıştırmayı açın.
3. Sayfanın altındaki **Artifacts → Atelier-Pad-debug-APK** dosyasını indirin.
4. ZIP'i çıkarın ve `app-debug.apk` dosyasını Android cihazınıza aktarın.
5. Cihazınızda bu dosyayı açın; gerekirse dosyayı açan uygulamaya “Bilinmeyen uygulamaları yükle” izni verin.

Bu APK geliştirme/test içindir ve debug anahtarıyla imzalanır; Google Play yayını için kişisel imza anahtarıyla release derlemesi gerekir. APK artifact'i 30 gün saklanır. İş akışı `workflow_dispatch` ile bu daldan yeniden çalıştırılabilir (GitHub arayüzündeki manuel çalıştırma düğmesinin görünmesi için workflow'un varsayılan dalda da bulunması gerekebilir).
