package com.google.jetpackcamera.ui.uistate

sealed interface UiSingleSelectableState<T> {
    data class Selectable<T>(val value: T) : UiSingleSelectableState<T>
    data class Disabled<T>(val value: T, val disabledReason: ReasonDisplayable) :
        UiSingleSelectableState<T>
}

interface ReasonDisplayable {
    val testTag: String
    val reasonTextResId: Int
    fun getDisplayMessage(context: android.content.Context): String {
        return context.getString(reasonTextResId)
    }
}