# Allow R8 to aggressively strip unused compose layout and viewfinder classes
-assumevalues class ** {
    *** getDebugMode() return false;
}
