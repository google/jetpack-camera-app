package com.google.jetpackcamera.core.camera;

import android.graphics.Rect;
import androidx.camera.core.CameraInfo;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class FocusMeteringTestHelper {
    public static MockedStatic<?> mockSensorRect(CameraInfo cameraInfo, Rect rect) {
        MockedStatic<CameraExtKt> mock = Mockito.mockStatic(CameraExtKt.class);
        mock.when(() -> CameraExtKt.getSensorRect(cameraInfo)).thenReturn(rect);
        return mock;
    }
}

