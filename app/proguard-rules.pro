# ── libxposed API 102 官方三条（缺第二条，开 R8 后模块直接不被识别）──
-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}

# 回调靠虚分派不需要保留方法名；若用注解或反射找回调，再补下面这条
-keepattributes RuntimeVisibleAnnotations

# ── 构建相关 ──
# androidx.window 是 compileOnly 依赖（运行时由系统提供扩展），不 dontwarn 会让
# minifyReleaseWithR8 直接失败：Missing class androidx.window.extensions.*
-dontwarn androidx.window.**
