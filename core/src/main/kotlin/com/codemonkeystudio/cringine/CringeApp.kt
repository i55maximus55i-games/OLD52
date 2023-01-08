package com.codemonkeystudio.cringine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.codemonkeystudio.old52.network.OldClient
import com.codemonkeystudio.old52.network.OldServer
import com.kotcrab.vis.ui.VisUI
import de.eskalon.commons.core.ManagedGame
import de.eskalon.commons.screen.transition.ScreenTransition
import de.eskalon.commons.screen.transition.impl.BlendingTransition
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.inject.Context
import ktx.inject.register
import ktx.scene2d.Scene2DSkin

open class CringeApp : ManagedGame<CringeScreen, ScreenTransition>() {

    val context: Context = Context()

    var firstScreen: String = ""
    var firstTransition: String = ""

    override fun create() {
        super.create()
        KtxAsync.initiate()
        VisUI.load()
        Scene2DSkin.defaultSkin = VisUI.getSkin()
        context.register {
            bindSingleton(this@CringeApp)
            bindSingleton(SpriteBatch())
            bindSingleton(ShapeRenderer())
            bindSingleton(AssetStorage())

            // Blyat
            bindSingleton(OldClient(context))
            bindSingleton(OldServer())
        }

        screenManager.apply {
            addScreen("EngineLoading", CringeLoadScreen(context))
            addScreenTransition("BlendingTransition", BlendingTransition(context.inject(), 0.25f))
        }

        setScreen(context, "EngineLoading", "BlendingTransition")
    }

    override fun render() {
        super.render()
        val client: OldClient = context.inject()
        val server: OldServer = context.inject()

        client.update()
        server.update(Gdx.graphics.deltaTime)
    }

    open fun createScreens() = Unit

    fun setScreen(context: Context, screen: String, transition: String) {
        val game: CringeApp = context.inject()
        game.screenManager.pushScreen(screen, transition)
    }

    override fun dispose() {
        super.dispose()
        val client: OldClient = context.inject()
        val server: OldServer = context.inject()
        client.socket.disconnect()
        server.server.stop()
    }

}