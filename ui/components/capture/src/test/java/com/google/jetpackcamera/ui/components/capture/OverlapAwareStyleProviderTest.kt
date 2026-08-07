package com.google.jetpackcamera.ui.components.capture

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OverlapAwareStyleProviderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overlapAware_aboveThreshold_providesBlack60() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            Box(Modifier.size(500.dp)) {
                CompositionLocalProvider(
                    LocalOverlapTargetBounds provides mutableStateOf(Rect(0f, 0f, 100f, 100f))
                ) {
                    OverlapAwareStyleProvider(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 10.dp)
                            .size(100.dp)
                    ) {
                        detectedStyle = LocalCameraControlBackgroundStyle.current
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(detectedStyle).isEqualTo(CameraControlBackgroundStyle.BLACK_60)
    }

    @Test
    fun overlapAware_belowThreshold_providesWhite20() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            Box(Modifier.size(500.dp)) {
                CompositionLocalProvider(
                    LocalOverlapTargetBounds provides mutableStateOf(Rect(0f, 0f, 100f, 100f))
                ) {
                    OverlapAwareStyleProvider(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 60.dp)
                            .size(100.dp)
                    ) {
                        detectedStyle = LocalCameraControlBackgroundStyle.current
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(detectedStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
    }

    @Test
    fun overlapAware_none_providesWhite20() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            Box(Modifier.size(500.dp)) {
                CompositionLocalProvider(
                    LocalOverlapTargetBounds provides mutableStateOf(Rect(0f, 0f, 100f, 100f))
                ) {
                    OverlapAwareStyleProvider(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 110.dp)
                            .size(100.dp)
                    ) {
                        detectedStyle = LocalCameraControlBackgroundStyle.current
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(detectedStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
    }
}
