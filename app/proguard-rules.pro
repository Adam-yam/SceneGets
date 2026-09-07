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
-keep class com.adamyam.scenegets.widget.chart.ChartWidgetReceiver { *; }
-keep class com.adamyam.scenegets.widget.news.NewsWidgetReceiver { *; }
-keep class com.adamyam.scenegets.widget.schedule.ScheduleWidgetReceiver { *; }

-keep class * extends androidx.glance.appwidget.action.ActionCallback {
    <init>();
    *;
}
