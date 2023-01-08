package com.codemonkeystudio.old52.screens

import com.codemonkeystudio.cringine.CringeScreen
import com.codemonkeystudio.old52.network.OldClient
import com.codemonkeystudio.old52.network.OldServer
import ktx.actors.onChange
import ktx.inject.Context
import ktx.scene2d.actors
import ktx.scene2d.textButton
import ktx.scene2d.textField
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visTable

class MainMenuScreen(context: Context) : CringeScreen(context) {

    val client : OldClient = context.inject()
    val server : OldServer = context.inject()

    init {
        uiStage.actors {
            visTable {
                setFillParent(true)
                center()

                visLabel("Server port")
                val portTextField = textField("1337")
                textButton("Start Server") {
                    onChange {
                        server.startServer(portTextField.text.toInt())
                        client.start("localhost:${portTextField.text}")
                    }
                }
                row()
                visLabel("Address")
                val addressTextField = textField("localhost:1337")
                textButton("Connect") {
                    onChange {
                        client.start(addressTextField.text)
                    }
                }
            }
        }
    }

}