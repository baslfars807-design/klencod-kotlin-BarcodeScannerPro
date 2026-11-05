# Barcode Scanner Pro - Google Style

تطبيق قارئ باركود خارق السلاسة، 100% بدون نت، بتصميم Material 3 مثل تطبيقات جوجل الأصلية.

## ✨ المميزات
- **سلس جدا جدا جدا:** CameraX + STRATEGY_KEEP_ONLY_LATEST + تحليل في background thread = 60fps ثابت
- **بدون نت نهائيا:** Google ML Kit Barcode 17.3.0 يشتغل كامل داخل الجهاز
- **تصميم جوجل الأصلي:** Material 3 Dynamic Color + TopAppBar + BottomSheet + FAB مثل Google Lens
- **يقرأ كل الأنواع:** QR, EAN-13, UPC-A/E, Code-128, Code-39, DataMatrix, PDF417, Aztec...
- **ميزات برو:** فلاش، اهتزاز، نسخ، مشاركة، فتح الرابط، تاريخ الفحص

## 🛠️ أحدث المكتبات (نوفمبر 2026)
- Kotlin 2.0.21
- AGP 8.7.0
- Compose BOM 2024.10.00 + Material3
- CameraX 1.4.1
- ML Kit 17.3.0
- Target SDK 35 (Android 15)

## 🚀 البناء في GitHub
1. ارفع هذا المجلد كامل لـ GitHub repo جديد
2. الـ workflow في `.github/workflows/android.yml` يبني الـ APK تلقائيا عند كل push
3. حمل الـ APK من Actions > Artifacts

## 📱 البناء محليا
1. افتح Android Studio Ladybug أو أحدث
2. Open > اختر مجلد BarcodeScannerPro
3. انتظر Sync (يحتاج نت أول مرة فقط)
4. شغل على جوالك: Run

## 🔒 الخصوصية
التطبيق لا يرسل أي بيانات. كل المعالجة Offline.