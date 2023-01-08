package com.codemonkeystudio.old52.network

import com.badlogic.gdx.Gdx
import com.codemonkeystudio.cringine.CringeApp
import io.socket.client.IO
import io.socket.client.Socket
import ktx.inject.Context

class OldClient(val context: Context) {

    var socket = IO.socket("http://localhost:1337")

    fun createEvents() {
        socket.on(Socket.EVENT_CONNECT) {
            Gdx.app.log("NetClient", "Connected ${socket.id()}")
            context.inject<CringeApp>().setScreen(context, "Game", "Doorway_0.5")
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Gdx.app.log("NetClient", "Disconnected ${socket.id()}")
            context.inject<CringeApp>().setScreen(context, "MainMenu", "Doorway_0.5")
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Gdx.app.log("NetClient", it[0].toString())
        }
    }

    fun start(address: String) {
        socket.disconnect()
        socket = IO.socket("http://$address")
        createEvents()
        socket.connect()
    }

    fun update() {

    }

}