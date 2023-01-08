@file:JvmName("Lwjgl3Launcher")

package com.codemonkeystudio.old52.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.codemonkeystudio.old52.Application

/** Launches the desktop (LWJGL3) application. */
fun main() {
    Lwjgl3Application(Application(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("OLD52")
        val displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
        setFullscreenMode(displayMode)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    })
}
