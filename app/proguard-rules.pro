# One UI blur is reached by name. R8 must not rewrite the strings or drop the call.
-keep class android.view.SemBlurInfo { *; }
-keep class android.view.SemBlurInfo$Builder { *; }
-keepclassmembers class android.view.View {
    void semSetBlurInfo(...);
}
