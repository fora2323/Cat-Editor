-repackageclasses
-dontwarn
-dontnote

-keep class io.github.rosemoe.sora.** { *; }
-keep interface io.github.rosemoe.sora.** { *; }
-keep class io.github.rosemoe.** { *; }

-keep class org.eclipse.tm4e.** { *; }
-keep class org.eclipse.tm4e.core.** { *; }
-keep class org.eclipse.tm4e.core.internal.oniguruma.** { *; }

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*