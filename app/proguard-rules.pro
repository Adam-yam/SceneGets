# kotlinx.serialization: 공식 권장 규칙 (https://github.com/Kotlin/kotlinx.serialization#android)
# ChartResponse/NewsResponse/ScheduleEvent 등 @Serializable 데이터 클래스와
# 컴파일러가 생성하는 $$serializer 컴패니언이 R8에 의해 지워지거나 이름이
# 바뀌지 않도록 보존한다.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.adamyam.scenegets.**$$serializer { *; }
-keepclassmembers class com.adamyam.scenegets.** {
    *** Companion;
}
-keepclasseswithmembers class com.adamyam.scenegets.** {
    kotlinx.serialization.KSerializer serializer(...);
}
