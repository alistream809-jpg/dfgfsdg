# RAMFLOW PRO

Kotlin + Jetpack Compose + Material 3 ile gerçek RAM/performans izleyici (Android 10+, root yok, sahte temizlik yok).
Paket: `com.ramflow.pro`

## APK'yı GitHub ile alma (bilgisayarsız olur)
1. GitHub'da yeni repo aç, bu klasörün **tüm içeriğini** (gizli `.github` klasörü dahil) yükle.
2. Repo → **Actions** → **Build RAMFLOW PRO APK** (push ile otomatik başlar; başlamazsa **Run workflow**).
3. Bitince çalışmanın sayfasında **Artifacts → RAMFLOW-PRO-APK** indir, zip'i aç.
4. `app-debug.apk` (veya `app-release.apk`) dosyasını telefona at ve kur ("bilinmeyen kaynaklara izin" gerekir).

## Android Studio ile
`File → Open` ile klasörü aç, Gradle sync bitince `Build → Build APK(s)`.
Komut satırı: `gradle assembleDebug` (Gradle 8.9 + JDK 17). Çıktı: `app/build/outputs/apk/debug/app-debug.apk`

## Uygulamada ilk kurulum
- **Uygulamalar** sekmesindeki bannerdan *Kullanım erişimi* ver (son kullanım, boyut, önbellek bilgisi için).
- Otomatik mod için bildirim iznini onayla.

## Dürüstlük notları
- Android, üçüncü taraf uygulamaların başka uygulamanın RAM'ini görmesine / zorla durdurmasına izin vermez.
- Boost, `killBackgroundProcesses` isteği gönderir (sistem karar verir, doğrulanamaz) ve zorla durdurma için seni Uygulama Bilgisi ekranına yönlendirir.
- Engellenen işlemler raporda "Yapılamadı" / "Senin işlemin gerekli" olarak görünür.
