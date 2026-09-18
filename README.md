# Pocket Option Android Scanner

Bu Android tətbiqi Android MediaProjection ilə istifadəçinin icazəsi əsasında ekran görüntülərini götürür və Render-dəki `/analyze-frame` endpointinə göndərir.

## Telefonda APK necə alınır?

1. Bu layihəni GitHub repository-yə yüklə.
2. `.github/workflows/build-apk.yml` avtomatik işləyəcək.
3. GitHub → Actions → `Build Android APK` → son run → Artifacts → `pocket-option-scanner-apk` yüklə.
4. ZIP-dən `app-debug.apk` çıxar və telefonda quraşdır.

## Render endpoint

MainActivity-də default:
https://pocket-signal-12.onrender.com/analyze-frame

Əgər Render URL dəyişibsə, tətbiqdə URL-i dəyiş.

## İstifadə

1. Tətbiqi aç.
2. Aktiv və müddət seç.
3. "Ekranı paylaş və scanneri başlat" bas.
4. Android ekran capture icazəsini təsdiqlə.
5. Pocket Option-a keç.
6. Scanner 2 saniyədə bir kadr göndərir.

## Vacib

- Avtomatik trade açmır.
- Screen capture Android-in sistem icazəsi ilə işləyir.
- Android 14+ hər capture sessiyası üçün istifadəçi razılığı tələb edir.
- 30s/1m analizləri qeyri-müəyyəndir; nəticəyə və qazanca zəmanət verilmir.
- Render backend hazırkı layihədə OpenAI vision analizini verir.
