# R8 rules for the release build.
#
# Most of what we depend on (Compose, DataStore, Health Services) ships its own
# consumer rules, so this file stays small on purpose. Add a rule only when a
# release build actually breaks, and say here what broke.

# Health Services moves data across a process boundary as protos/parcelables.
# Keep its data classes intact rather than trusting reflection to survive
# renaming.
-keep class androidx.health.services.client.data.** { *; }
-keep class androidx.health.services.client.proto.** { *; }

# Our domain and application layers are pure data + behaviour with no
# reflection, so they shrink freely. Nothing to keep.

# Keep line numbers so a crash report from the watch is readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
