# Temel Android kuralları
    -include getDefaultProguardFile('proguard-android-optimize.txt')

    # View Binding ve Data Binding
    -keep class * extends androidx.databinding.**
    -keep class * extends android.databinding.**

    # JSON (Gson)
    -keep class com.google.gson.** { *; }
    -keepattributes Signature

    # Hata ayıklama bilgileri (isteğe bağlı)
    -keepattributes SourceFile,LineNumberTable
    -renamesourcefileattribute SourceFile