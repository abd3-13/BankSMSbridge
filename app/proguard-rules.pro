# Keep all classes (safe default for now)
-keep class * { *; }

# Aggressive performance optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-allowaccessmodification
-repackageclasses ''
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-verbose
