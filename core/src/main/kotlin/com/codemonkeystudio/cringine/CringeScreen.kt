package com.codemonkeystudio.cringine

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import de.eskalon.commons.screen.ManagedScreen
import ktx.assets.async.AssetStorage
import ktx.assets.disposeSafely
import ktx.inject.Context

open class CringeScreen(val context: Context) : ManagedScreen() {

    internal val assetStorage: AssetStorage = context.inject()

    internal val uiStage: Stage = Stage(ScreenViewport(), context.inject<SpriteBatch>())
    internal val ecsEngine: PooledEngine = PooledEngine()

    internal var screenColor: Color = Color.BLACK
    override fun getClearColor(): Color { return screenColor }

    override fun create() {
        addInputProcessor(uiStage)
    }

    override fun render(delta: Float) {
        ecsEngine.update(delta)
        uiStage.apply {
            act(delta)
            draw()
        }
    }

    override fun resize(width: Int, height: Int) {
        uiStage.viewport.update(width,height, true)
    }

    override fun hide() {}

    override fun dispose() {
        uiStage.disposeSafely()
    }

}