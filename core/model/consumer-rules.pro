# Allow R8 to aggressively strip unused debug settings and dead code downstream
-assumevalues class com.google.jetpackcamera.model.DebugSettings {
    *** isDebugModeEnabled() return false;
}
