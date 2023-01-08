package com.codemonkeystudio.old52

import com.badlogic.gdx.math.Interpolation
import com.codemonkeystudio.cringine.CringeApp
import com.codemonkeystudio.old52.screens.GameScreen
import com.codemonkeystudio.old52.screens.MainMenuScreen
import de.eskalon.commons.screen.transition.impl.GLTransitionsShaderTransition
import ktx.assets.toInternalFile

class Application : CringeApp() {

    override fun createScreens() {
        screenManager.apply {
            addScreen("MainMenu", MainMenuScreen(context))
            addScreen("Game", GameScreen(context))

            addScreenTransition(
                "Doorway_0.5",
                GLTransitionsShaderTransition(0.5f, Interpolation.circle).apply {
                    compileGLTransition("transitions/Doorway.glsl".toInternalFile().readString())
                }
            )
        }

        firstScreen = "MainMenu"
        firstTransition = "Doorway_0.5"
    }

}
