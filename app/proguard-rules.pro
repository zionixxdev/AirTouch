# AirTouch ProGuard rules.
# Minification is disabled for the debug and release build types, so these rules
# are only a safety net if minification is enabled later.

# MediaPipe Tasks uses reflection / JNI for its option builders.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
