-dontwarn
-dontnote

# Keep attributes required for reflection, annotations, generics, and line numbers
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable

# =================================================================
# Sora Editor core and Language-TextMate module
# =================================================================
-keep class io.github.rosemoe.** { *; }
-keep interface io.github.rosemoe.** { *; }
-keep class io.github.rosemoe.sora.** { *; }
-keep interface io.github.rosemoe.sora.** { *; }
-keepclassmembers class io.github.rosemoe.sora.** { *; }

# TextMate language, theme, and grammar models deserialized via Gson
-keep class io.github.rosemoe.sora.langs.textmate.** { *; }
-keep interface io.github.rosemoe.sora.langs.textmate.** { *; }
-keepclassmembers class io.github.rosemoe.sora.langs.textmate.** { *; }
-keep class io.github.rosemoe.sora.langs.textmate.registry.model.** { *; }
-keepclassmembers class io.github.rosemoe.sora.langs.textmate.registry.model.** { *; }
-keep class io.github.rosemoe.sora.langs.textmate.registry.reader.** { *; }
-keepclassmembers class io.github.rosemoe.sora.langs.textmate.registry.reader.** { *; }

# =================================================================
# Eclipse TM4E (TextMate core parser, grammar, themes, regex tokenization)
# =================================================================
-keep class org.eclipse.tm4e.** { *; }
-keep interface org.eclipse.tm4e.** { *; }
-keepclassmembers class org.eclipse.tm4e.** { *; }
-dontwarn org.eclipse.tm4e.**

# =================================================================
# Joni & JCodings (Java port of Oniguruma regex used by TM4E)
# =================================================================
-keep class org.joni.** { *; }
-keep interface org.joni.** { *; }
-keepclassmembers class org.joni.** { *; }
-dontwarn org.joni.**

-keep class org.jruby.joni.** { *; }
-keep interface org.jruby.joni.** { *; }
-keepclassmembers class org.jruby.joni.** { *; }
-dontwarn org.jruby.joni.**

-keep class org.jcodings.** { *; }
-keep interface org.jcodings.** { *; }
-keepclassmembers class org.jcodings.** { *; }
-dontwarn org.jcodings.**

-keep class org.jruby.jcodings.** { *; }
-keep interface org.jruby.jcodings.** { *; }
-keepclassmembers class org.jruby.jcodings.** { *; }
-dontwarn org.jruby.jcodings.**

# =================================================================
# SnakeYAML engine (used by TM4E to parse YAML grammars/themes)
# =================================================================
-keep class org.snakeyaml.engine.** { *; }
-keep interface org.snakeyaml.engine.** { *; }
-keepclassmembers class org.snakeyaml.engine.** { *; }
-dontwarn org.snakeyaml.engine.**

# =================================================================
# Gson reflection and serialized fields
# =================================================================
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# =================================================================
# JGit (Git operations)
# =================================================================
-keep class org.eclipse.jgit.** { *; }
-keep interface org.eclipse.jgit.** { *; }
-keepclassmembers class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn java.awt.**
-dontwarn org.slf4j.**