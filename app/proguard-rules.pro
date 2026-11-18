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

# ── 防检测相关 ──

# 1) 日志彻底移除：HyLog 里的 ENABLED 是 const false，Kotlin 已把分支折叠，
#    这里再保证空的 log 调用和残留字符串一并删掉，release 包里不存在任何日志字面量。
-assumenosideeffects class com.halo.mirror.core.HyLog {
    public static void i(java.lang.String);
    public static void w(java.lang.String);
}

# 2) 诊断代码不进 release 包（Probe 只在排错时手动调用）
-assumenosideeffects class com.halo.mirror.diag.Probe {
    public static void panel(***);
    public static void window(***);
    public static void tree(***);
}

# 3) LSPosed 日志 tag 不要留成可读字符串
-adaptclassstrings

# 4) 保留反射用到的字段名：ConstraintLayout 的布局参数按字段名反射设置
-keepclassmembers class androidx.constraintlayout.widget.ConstraintLayout$LayoutParams {
    int width;
    float horizontalWeight;
    float horizontalBias;
    int startToStart;
    int endToEnd;
    int startToEnd;
    int endToStart;
    int leftToLeft;
    int rightToRight;
    int leftToRight;
    int rightToLeft;
}
