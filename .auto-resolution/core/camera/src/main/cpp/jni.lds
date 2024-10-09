VERS_1.0 {
  # Export JNI symbols.
  global:
    Java_*;
    JNI_OnLoad;

  # Hide everything else.
  local:
    *;
};
