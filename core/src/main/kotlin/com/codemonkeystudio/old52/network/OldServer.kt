package com.codemonkeystudio.old52.network

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.codemonkeystudio.old52.objects.Bullet
import com.codemonkeystudio.old52.objects.Enemy
import com.codemonkeystudio.old52.objects.Player
import com.codemonkeystudio.old52.objects.WeaponConstants
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import kotlinx.coroutines.InternalCoroutinesApi
import org.json.JSONObject
import kotlin.math.sqrt

class OldServer {

    val config = Configuration().apply {
        port = 1337
    }

    val server = SocketIOServer(config)

    val players = HashMap<String, Player>()
    val enemies = ArrayList<Enemy>()
    val bullets = ArrayList<Bullet>()

    val enemySpeed = 900f

    fun createEvents() {
        server.addConnectListener {
            Gdx.app.log("NetServer", "Client connected ${it.sessionId}")
            players[it.sessionId.toString()] = Player()
            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_PLAYER_CONNECTED, it.sessionId.toString())
        }
        server.addDisconnectListener {
            Gdx.app.log("NetServer", "Client disconnected ${it.sessionId}")
            // TODO: remove player object
        }

        server.addEventListener(OldNetworkEvents.EVENT_REQUEST_PLAYER_LIST, String::class.java) { client, data, _ ->
            val json = JSONObject()
            json.put("Count", players.size)
            var c = 0
            for (i in players) {

                json.put("ID$c", i.key)
                json.put("x$c", i.value.position.x)
                json.put("y$c", i.value.position.y)
                json.put("alive$c", i.value.alive)
                json.put("w$c", WeaponConstants.getWeaponInt(i.value.currentWeapon))

                c++
            }
            client.sendEvent(OldNetworkEvents.EVENT_GET_PLAYER_LIST, json.toString())
        }
        server.addEventListener(OldNetworkEvents.EVENT_UPDATE_PLAYER_POS, String::class.java) { client, data, _ ->
            val json = JSONObject(data)
            val id = json.getString("ID")
            val x = json.getFloat("x")
            val y = json.getFloat("y")
            val cross = json.getFloat("crosshair")
            val player = players[id]
            if (player != null) {
                player.position.x = x
                player.position.y = y
                player.crossHairAngle = cross
            }
            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_UPDATE_PLAYER_POS, data)
        }
        server.addEventListener(OldNetworkEvents.EVENT_PLAYER_SHOOT, String::class.java) { client, data, _ ->
            val json = JSONObject(data)
            bullets.add(Bullet().apply {
                pos.x = json.getFloat("x")
                pos.y = json.getFloat("y")
                owner = json.getString("id")
                angle = json.getFloat("angle")
                speed = json.getFloat("speed")
                hurt =  json.getInt("hurt")
            })
            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_PLAYER_SHOOT, data)
        }
        server.addEventListener(OldNetworkEvents.EVENT_RESPAWN, String::class.java) { client, data, _ ->
            val json = JSONObject(data)
            val id = json.getString("player")
            val x = json.getFloat("x")
            val y = json.getFloat("y")
            val player = players[id]
            if (player != null) {
                player.alive = true
                player.position.x = x
                player.position.y = y
                player.hp = 100

                player.gold = 0
            }
            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_RESPAWN, data)
        }
        server.addEventListener(OldNetworkEvents.EVENT_CHANGE_PLAYER_WEAPON, String::class.java) { client, data, _ ->
            val json = JSONObject()
            json.put("id", client.sessionId.toString())
            json.put("w", data)
            players[client.sessionId.toString()]?.currentWeapon = WeaponConstants.getWeaponEnum(data.toInt())
            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_CHANGE_PLAYER_WEAPON, json.toString())
        }

        server.addEventListener(OldNetworkEvents.EVENT_ENEMIES_REQUEST, String::class.java) { client, data, _ ->
            val json = JSONObject()
            json.put("Count", enemies.size)
            for ((i, enemy) in enemies.withIndex()) {

                json.put("ID$i", i)
                json.put("x$i", enemy.position.x)
                json.put("y$i", enemy.position.y)
                json.put("alive$i", enemy.alive)
            }
            client.sendEvent(OldNetworkEvents.EVENT_ENEMIES_REQUEST, json.toString())
        }

        server.addEventListener(OldNetworkEvents.EVENT_NO_GOLD, String::class.java) { client, data, _ ->
            val player = players[client.sessionId.toString()]
            if (player != null) player.gold = 0
        }
    }

    fun startServer(port: Int) {
        players.clear()
        config.port = port
        createEvents()
        server.start()
        Gdx.app.log("NetServer", "Server started")

        enemies.add(Enemy())
    }

    fun update(delta: Float) {
        updateBullets(delta)
        updateEnemies(delta)
    }

    fun updateBullets(delta: Float) {
        try {
            for (bullet in bullets) {
                bullet.pos.add(Vector2(bullet.speed * delta, 0f).setAngleDeg(bullet.angle))

                for (j in players) {
                    val player = j.value
                    if (j.key != bullet.owner && player.position.dst(bullet.pos) < 200f && player.alive) {
                        playerHit(j.key, bullet)
                    }
                }
                for ((index, enemy) in enemies.withIndex()) {
                    if (enemy.position.dst(bullet.pos) < 100f && enemy.alive) {
                        enemyHit(index, bullet)
                    }
                }
            }
        } catch (e: Exception) {}
    }
    var enemyRespawnDelay = 2.2f
    fun updateEnemies(delta: Float) {
        enemyRespawnDelay -= delta
        if (enemyRespawnDelay < 0f) {
            enemyRespawnDelay = if (players.size >= 1) 3f / players.size.toFloat() else 3f

            val i = enemies.size
            val enemy = Enemy()
            val json = JSONObject()
            json.put("id", i)
            json.put("x", enemy.position.x)
            json.put("y", enemy.position.y)
            enemies.add(enemy)
            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_ENEMY_SPAWN, json.toString())
        }

        for ((i, enemy) in enemies.withIndex()) {
            if (enemy.alive) {

                var target = Vector2(99999f, 999999f)

                for (j in players) {
                    val player = j.value
                    if (!player.alive) continue

                    if (enemy.position.dst(player.position) < enemy.position.dst(target)) target = player.position.cpy()
                }

                enemy.position.add(target.sub(enemy.position).setLength(enemySpeed * delta))

                val json = JSONObject()
                json.put("id", i)
                json.put("x", enemy.position.x)
                json.put("y", enemy.position.y)
                server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_ENEMY_POS, json.toString())

                for (j in players) {
                    val player = j.value
                    if (enemy.position.dst(player.position) < 200f) {
                        if (player.alive) {
                            enemy.alive = false
                            player.hp -= 20

                            val json = JSONObject()
                            json.put("id", j.key)
                            json.put("hp", player.hp)
                            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_ENEMY_DEAD, i.toString())
                            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_HP_CHANGE, json.toString())
                            if (player.hp <= 0) {
                                player.alive = false
                                server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_PLAYER_DEAD, j.key)
                            }
                        }
                    }
                }
            }
        }
    }

    fun playerHit(playerID: String, bullet: Bullet) {
        val player = players[playerID]
        if (player != null && bullet.alive && player.alive) {
            bullet.alive = false
            player.hp -= bullet.hurt

            val json = JSONObject()
            json.put("id", playerID)
            json.put("hp", player.hp)
            server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_HP_CHANGE, json.toString())
            if (player.hp <= 0) {
                player.alive = false
                server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_PLAYER_DEAD, playerID)
                stealGold(playerID, bullet.owner)
            }
        }
    }

    fun enemyHit(key: Int, bullet: Bullet) {
        val enemy = enemies[key]
        if (bullet.alive && enemy.alive) {
            bullet.alive = false
            enemy.hp -= bullet.hurt
            if (enemy.hp <= 0) {
                enemy.alive = false
                server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_ENEMY_DEAD, key.toString())

                addGoldToPlayer(bullet.owner, 1)
            }
        }
    }

    private fun addGoldToPlayer(playerID: String, count: Int) {
        for (i in players) {
            if (i.key == playerID) {
                i.value.gold++
            }
        }
        val json = JSONObject()
        json.put("id", playerID)
        json.put("count", count)
        server.broadcastOperations.sendEvent(OldNetworkEvents.EVENT_GOLD_GET, json.toString())
    }

    private fun stealGold(from: String, to: String) {
        val playerA = players[from]

        if (playerA != null) {
            addGoldToPlayer(to, playerA.gold)
            playerA.gold = 0
        }
    }

}