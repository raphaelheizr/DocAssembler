package dev.heizer

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.heizer.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DocAssembler",
    ) {
        App()
    }
}