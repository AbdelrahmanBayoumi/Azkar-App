# ProGuard rules for the release build. The compose desktop DSL runs this with
# shrink alone - no obfuscation (the app is open source, so renaming buys nothing
# and only breaks name lookups) and no optimization (it rewrote a LocalVariableTable
# entry in material3's SwitchDefaults into a malformed class that JDK 21+ rejects).
# So the job here is pruning dead library classes, notably the ~37MB of unused
# Material icons in material-icons-extended-desktop, while keeping the parts that
# are only ever reached by name.
#
# The Compose plugin injects its own defaults for compose/skiko; rules here cover
# what it cannot see. :app:collectProguardRules scans the runtime classpath for
# META-INF/services and generates a keep per provider into
# build/compose/proguard-generated.pro, minus the ones already held by a rule in
# this file or by the plugin defaults, so a dependency bump cannot silently drop
# one.

# ProGuard 7.8.0 reads Kotlin metadata up to version 2.3, while this project
# compiles with Kotlin 2.4, and reading the module file then fails the build,
# so let ProGuard skip it.
-dontprocesskotlinmetadata

# Coroutine implementations call Job's default methods through a direct superinterface.
# ProGuard otherwise drops Job as redundant when another implemented interface extends it.
-keep interface kotlinx.coroutines.Job

# Exposed builds the entities in com.azkar.data.table.entity through
# kotlin-reflect (`primaryConstructor` plus `callBy`), which needs our classes'
# metadata and constructors intact. Without this it throws
# `KotlinReflectionInternalError: Could not compute caller` the first time
# anything loads the favourites, the hadith or the collection rail. The app's own
# code is a few megabytes, so keeping all of it costs nothing measurable.
-keep class com.azkar.** { *; }

# DataStore's `PreferencesProto` is a protobuf-lite message: at runtime it
# resolves its own fields by name, so dropping a field breaks it with
# `Field preferences_ for ... not found` while reading the settings file. DataStore
# 1.2.x ships its own repackaged copy of the protobuf runtime, so the messages
# extend `androidx.datastore.preferences.protobuf.GeneratedMessageLite` rather than
# the usual `com.google.protobuf` one.
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }
-keep class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite$Builder { *; }

# H2 is loaded reflectively (Class.forName("org.h2.Driver")) and its SQLDialect
# / JdbcConnection classes are picked up by string name.
-keep class org.h2.** { *; }

# Logback is configured by text (logback.xml) and its SPI (ServiceLoader).
# The configured ConsoleAppender does not use Logback's optional JLine ANSI integration.
-keep class ch.qos.logback.** { *; }
-dontwarn org.jline.jansi.AnsiConsole

# Ktor resolves engines and serialization formats through ServiceLoader and
# string-named configuration keys.
-keep class io.ktor.** { *; }

# JNA discovers native symbols via string lookups on interfaces
# (tray / Linux bus glue), so keep its infrastructure whole.
-keep class com.sun.jna.** { *; }

# FileKit's Linux file picker calls dbus-java APIs reflectively. Keeping only the
# ServiceLoader transport provider is not enough: ProGuard otherwise removes
# members such as Properties.Set that the portal client resolves by name.
-keep class org.freedesktop.dbus.** { *; }

# The GNOME prayer indicator exposes D-Bus methods, properties, and menu tuples by reflection.
-keep interface com.azkar.ui.shell.GnomePrayer* extends org.freedesktop.dbus.interfaces.DBusInterface { *; }
-keep interface com.azkar.ui.shell.GnomeShellPanelService extends org.freedesktop.dbus.interfaces.DBusInterface { *; }
-keep class com.azkar.ui.shell.GnomePrayer* extends org.freedesktop.dbus.Container { *; }
-keep class com.azkar.ui.shell.GnomePrayerMenu$LayoutUpdated { *; }

# The HWND class uses a method handle to its native window procedure.
-keepclassmembers class com.azkar.ui.shell.WindowsTaskbarWidget {
    private long windowProc(java.lang.foreign.MemorySegment, int, long, long);
}

# AutoLaunch selects the macOS backend by class name; JNI calls back into its bridges.
-keep class dev.nucleusframework.autolaunch.** { *; }
-keep class dev.nucleusframework.servicemanagement.** { *; }

# kotlinx.serialization's generated serializers and the reflective lookup of
# serializers from Companion properties.
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep interface kotlinx.serialization.KSerializer { *; }

# These libraries ship optional integration classes in their main jars, but the
# corresponding APIs are not part of this desktop app: Logback's servlet, mail,
# XZ, and Janino features; Hikari's Hibernate/metrics/Javassist adapters; and
# H2's web, Lucene, geometry, and OSGi integrations. The warned methods are on
# Java's signature-polymorphic MethodHandle/VarHandle types; ProGuard cannot
# resolve their call-site-specific signatures even though the JDK classes exist.
-dontwarn com.codahale.metrics.**
-dontwarn io.dropwizard.metrics5.**
-dontwarn io.micrometer.core.**
-dontwarn io.prometheus.client.**
-dontwarn jakarta.mail.**
-dontwarn jakarta.servlet.**
-dontwarn javax.servlet.**
-dontwarn javassist.**
-dontwarn java.lang.invoke.MethodHandle
-dontwarn java.lang.invoke.VarHandle
-dontwarn org.apache.lucene.**
-dontwarn org.codehaus.commons.compiler.**
-dontwarn org.codehaus.janino.**
-dontwarn org.hibernate.**
-dontwarn org.locationtech.jts.**
-dontwarn org.osgi.**
-dontwarn org.tukaani.xz.**
-dontwarn org.h2.server.web.**

# Generic signatures + annotations are needed by serialization and DI lookups.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,SourceFile,LineNumberTable

# JavaFX selects Glass/Prism/toolkit implementations using constructed class names.
# Media's native backend also calls these implementation classes through JNI.
-keep class com.sun.glass.** { *; }
-keep class com.sun.javafx.tk.** { *; }
-keep class com.sun.prism.** { *; }
-keep class com.sun.scenario.** { *; }
-keep class com.sun.media.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class javafx.** {
    native <methods>;
}
