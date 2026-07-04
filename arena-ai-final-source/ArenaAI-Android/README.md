# 🏟️ Arena.ai — Android App

<p align="center">
  <img src="app/src/main/res/drawable/ic_arena_logo.xml" width="120" alt="Arena.ai Logo">
</p>

یک اپلیکیشن اندرویدی حرفه‌ای برای [Arena.ai](https://arena.ai) — پلتفرم مقایسه و چت با هوش مصنوعی.

---

## ✨ ویژگی‌ها

| ویژگی | توضیحات |
|-------|---------|
| 🌐 **وب‌ویو حرفه‌ای** | نمایش کامل وب‌سایت arena.ai با عملکرد روان |
| 🔄 **کشیدن برای بازنشانی** | Pull-to-refresh برای به‌روزرسانی محتوا |
| 🧭 **ناوبری پایینی** | دسترسی سریع به خانه، چت، کاوش و پروفایل |
| 🌙 **حالت تاریک** | پشتیبانی کامل از Dark Mode |
| 🔔 **اعلان‌ها** | دریافت نوتیفیکیشن از طریق Firebase |
| 📤 **اشتراک‌گذاری** | به اشتراک‌گذاری صفحات با دوستان |
| 🔒 **امنیت** | Network Security Config و HTTPS-only |
| 📸 **آپلود فایل** | پشتیبانی از انتخاب فایل و دوربین |
| 🎬 **تمام‌صفحه** | پشتیبانی از ویدیوی تمام‌صفحه |
| ⚙️ **تنظیمات** | مدیریت حافظه نهان، حالت تاریک و بیشتر |
| 🚀 **صفحه اسپلش** | صفحه ورود زیبا با انیمیشن |
| 📱 **لینک عمیق** | باز شدن لینک‌های arena.ai در اپلیکیشن |

---

## 🛠️ فناوری‌ها

- **Language:** Java 17
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Build System:** Gradle 8.5 + AGP 8.2.0
- **Architecture:** MVVM-ready, Single Activity pattern

### کتابخانه‌ها

| کتابخانه | نسخه | کاربرد |
|----------|-------|--------|
| AndroidX AppCompat | 1.6.1 | سازگاری نسخه‌های قدیمی |
| Material Components | 1.11.0 | طراحی Material Design 3 |
| AndroidX WebKit | 1.9.0 | WebView مدرن |
| SwipeRefreshLayout | 1.1.0 | کشیدن برای بازنشانی |
| Navigation Component | 2.7.6 | ناوبری بین صفحات |
| Firebase Messaging | 23.4.0 | پوش نوتیفیکیشن |
| Preference | 1.2.1 | صفحه تنظیمات |

---

## 📂 ساختار پروژه

```
ArenaAI-Android/
├── app/
│   ├── build.gradle                    # تنظیمات بیلد اپ
│   ├── proguard-rules.pro              # قوانین ProGuard
│   └── src/main/
│       ├── AndroidManifest.xml         # مانیفست اپلیکیشن
│       ├── java/com/arena/ai/app/
│       │   ├── SplashActivity.java     # صفحه اسپلش
│       │   ├── MainActivity.java       # اکتیویتی اصلی (WebView)
│       │   ├── SettingsActivity.java   # صفحه تنظیمات
│       │   └── ArenaFirebaseMessagingService.java  # سرویس نوتیفیکیشن
│       └── res/
│           ├── drawable/               # آیکون‌ها و گرافیک
│           ├── layout/                 # لایه‌های UI
│           ├── menu/                   # منوها
│           ├── values/                 # رنگ‌ها، رشته‌ها، استایل‌ها
│           ├── xml/                    # تنظیمات و امنیت شبکه
│           └── mipmap-*/              # آیکون‌های اپلیکیشن
├── build.gradle                        # تنظیمات بیلد پروژه
├── settings.gradle                     # تنظیمات Gradle
├── gradle.properties                   # خصوصیات Gradle
└── README.md                           # این فایل
```

---

## 🚀 نحوه بیلد

### پیش‌نیازها

- Android Studio Hedgehog یا جدیدتر
- JDK 17
- Android SDK 34

### مراحل

```bash
# 1. کلون کردن پروژه
git clone <repo-url>
cd ArenaAI-Android

# 2. بیلد با Gradle
./gradlew assembleDebug

# 3. فایل APK خروجی در مسیر زیر
# app/build/outputs/apk/debug/app-debug.apk
```

### بیلد Release

```bash
# نیاز به keystore برای امضای دیجیتال
./gradlew assembleRelease
```

---

## 🔧 تنظیمات

### Firebase

برای فعال‌سازی نوتیفیکیشن، فایل `google-services.json` را در پوشه `app/` قرار دهید.

### Deep Link

اپلیکیشن به صورت خودکار لینک‌های `https://arena.ai` را باز می‌کند. این قابلیت در AndroidManifest پیکربندی شده است.

---

## 🎨 تم رنگی

| رنگ | کد | کاربرد |
|-----|-----|--------|
| Primary | `#6C5CE7` | رنگ اصلی برند |
| Secondary | `#A29BFE` | رنگ ثانویه |
| Accent | `#FD79A8` | رنگ تاکیدی |
| Background | `#0F0F1A` | پس‌زمینه |
| Surface | `#1A1A2E` | سطح کارت‌ها |
| Text Primary | `#EAEAEA` | متن اصلی |

---

## 📱 سازگاری

- ✅ Android 7.0 (API 24) و بالاتر
- ✅ پشتیبانی از RTL (فارسی، عربی و ...)
- ✅ سازگار با Android 14
- ✅ بهینه‌شده برای تبلت و گوشی
- ✅ پشتیبانی از حالت تاریک سیستم

---

## 📄 مجوز

این پروژه متعلق به Arena.ai است.

---

<div align="center">
  ساخته شده با ❤️ برای Arena.ai
</div>
