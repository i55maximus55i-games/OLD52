package com.codemonkeystudio.cringine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.kotcrab.vis.ui.widget.VisProgressBar
import ktx.assets.async.AssetStorage
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import ktx.inject.Context
import ktx.scene2d.actors
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visProgressBar
import ktx.scene2d.vis.visTable

class CringeLoadScreen(context: Context) : CringeScreen(context) {

    private val texture: Texture = Texture("Cringine.png")
    private val loadingProgressBar: VisProgressBar
    private var changeScreen = true

    init {
        uiStage.actors {
            visTable {
                setFillParent(true)

                visImage(texture)
                row()
                loadingProgressBar = visProgressBar(min = 0f, max = 1f, step = 0.001f)
            }
        }
    }

    override fun create() {
        super.create()
        screenColor = Color.WHITE
        loadAssets(assetStorage)
    }

    override fun render(delta: Float) {
        super.render(delta)
        loadingProgressBar.value = assetStorage.progress.percent
        if (assetStorage.progress.isFinished && changeScreen) {
            changeScreen = false
            val app = context.inject<CringeApp>()
            app.createScreens()
            app.setScreen(context, context.inject<CringeApp>().firstScreen, context.inject<CringeApp>().firstTransition)
        }
    }

    override fun dispose() {
        super.dispose()
        texture.disposeSafely()
    }

    fun loadAssets(assetStorage: AssetStorage) {
        val assetsListFile = "assets.txt".toInternalFile()
        for (i in assetsListFile.readString().split("\n")) {
            val assetFile = i.toInternalFile()
            if (assetFile.exists()) {
                when (assetFile.extension()) {
                    "png" -> assetStorage.loadAsync<Texture>(assetFile.path())
                    "jpg" -> assetStorage.loadAsync<Texture>(assetFile.path())
                    "wav" -> assetStorage.loadAsync<Sound>(assetFile.path())
                }
            }
        }
    }

}