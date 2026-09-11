# R8 rules for the phone companion's release build.
#
# Compose, Play services and the Wearable API ship their own consumer rules.
# Add a rule only when a release build actually breaks, and say what broke.

# Keep line numbers so a crash report from the phone is readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
