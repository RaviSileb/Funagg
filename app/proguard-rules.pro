# Jsoup optionally integrates with re2j; it is not bundled.
-dontwarn com.google.re2j.**
-dontwarn org.jspecify.annotations.**

# OkHttp platform adapters that are not present on Android.
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
