/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jetpackcamera.ui

import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute
import com.google.jetpackcamera.permissions.navigation.PermissionsRoute
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [systemBarsPolicyFor].
 *
 * The important cases here are the destinations that declare navigation arguments: their
 * `NavDestination.route` is the route *pattern*, so it still contains unresolved `{placeholder}`
 * query parameters. Matching on the raw string would silently fall through to the default policy.
 */
class SystemBarsPolicyTest {

    private val previewRouteWithArgs = "$PreviewRoute" +
        "?externalCaptureMode={externalCaptureMode}" +
        "&reviewAfterCapture={reviewAfterCapture}" +
        "&captureUris={captureUris}" +
        "&debugSettings={debugSettings}"

    private val permissionsRouteWithArgs =
        "$PermissionsRoute?requestable_permissions={requestable_permissions}"

    @Test
    fun systemBarsPolicyFor_returnsExpectedPolicyPerRoute() {
        val cases = listOf(
            // Route pattern as reported by navigation for a destination with arguments.
            previewRouteWithArgs to SystemBarsPolicy.HideStatusBar,
            // Bare base route, e.g. when navigating without any arguments.
            "$PreviewRoute" to SystemBarsPolicy.HideStatusBar,
            Routes.POST_CAPTURE_ROUTE to SystemBarsPolicy.HideStatusBar,
            Routes.SETTINGS_ROUTE to SystemBarsPolicy.ShowAll,
            permissionsRouteWithArgs to SystemBarsPolicy.ShowAll,
            "$PermissionsRoute" to SystemBarsPolicy.ShowAll,
            null to SystemBarsPolicy.ShowAll,
            "someUnknownRoute" to SystemBarsPolicy.ShowAll,
            "someUnknownRoute?arg={arg}" to SystemBarsPolicy.ShowAll
        )

        cases.forEach { (route, expected) ->
            assertEquals(
                "Unexpected policy for route: $route",
                expected,
                systemBarsPolicyFor(route)
            )
        }
    }

    @Test
    fun systemBarsPolicyFor_doesNotMatchRoutesThatMerelyStartWithACaptureRoute() {
        assertEquals(
            SystemBarsPolicy.ShowAll,
            systemBarsPolicyFor("${PreviewRoute}Something")
        )
    }
}
