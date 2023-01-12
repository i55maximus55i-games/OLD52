package com.codemonkeystudio.old52.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.codemonkeystudio.cringine.CringeScreen
import com.codemonkeystudio.old52.network.OldClient
import com.codemonkeystudio.old52.network.OldNetworkEvents
import com.codemonkeystudio.old52.objects.Bullet
import com.codemonkeystudio.old52.objects.Enemy
import com.codemonkeystudio.old52.objects.Player
import com.codemonkeystudio.old52.objects.WeaponConstants
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisProgressBar
import com.kotcrab.vis.ui.widget.VisTable
import ktx.assets.toInternalFile
import ktx.graphics.use
import ktx.inject.Context
import ktx.math.plus
import ktx.math.times
import ktx.scene2d.actors
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.vis.visImage
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visProgressBar
import ktx.scene2d.vis.visTable
import org.json.JSONObject
import kotlin.math.min

class GameScreen(context: Context) : CringeScreen(context) {

    val fov = 3000f
    val camera = OrthographicCamera().apply {
        position.x = 0f
        position.y = 0f
        setToOrtho(false)
        update()
    }
    val spriteBatch: SpriteBatch = context.inject()

    val back: Texture = assetStorage["back.jpg"]
    val stback: Texture = assetStorage["stback.png"]
    val bulletTexture: Texture = assetStorage["bullet.png"]
    val enemyTexture: Texture = assetStorage["enemy.png"]
    val crosshairTexture: Texture = assetStorage["crosshair.png"]
    val playerAliveTexture: Texture = assetStorage["player/PlayerAlive.png"]
    val playerDeadTexture: Texture = assetStorage["player/PlayerDead.png"]
    val weaponMp5: Texture = assetStorage["weapons/mp5.png"]
    val weaponGlock: Texture = assetStorage["weapons/glock.png"]
    val weaponSawedOff: Texture = assetStorage["weapons/sa.png"]
    val weaponAk: Texture = assetStorage["weapons/Ak74.png"]
    val goldTexture: Texture = assetStorage["gold.png"]
    val tengeTexture: Texture = assetStorage["tenge.jpg"]

    val client: OldClient = context.inject()

    val respawnTable: VisTable
    val respawnScoreLabel: VisLabel
    val statusTable: VisTable
    val respawnProgressBar: VisProgressBar
    val ammoProgressBar: VisProgressBar
    val weaponLabel: VisLabel
    val ammoLabel: VisLabel
    val hpLabel: VisLabel
    val huy1: VisImage
    val huy2: VisImage
    val shopTable: VisTable
    val moneyTable: VisTable
    val moneyLabel: VisLabel
    val goldLabel: VisLabel

    var respawnDelay = 1.5f
    var respawnTimer = respawnDelay

    val players = HashMap<String, Player>()
    val bullets = ArrayList<Bullet>()
    val enemies = HashMap<Int, Enemy>()
    val playerSpeed = 700f
    val mapSize = 10000f

//    val deathSounds = ArrayList<Sound>().apply {
//        println("sounds/death1.ogg".toInternalFile().exists())
//        add(assetStorage["sounds/death1.ogg"])
//        for (i in 1..5) {
//            add(assetStorage["sounds/death$i.ogg"])
//        }
//    }

    init {
        uiStage.actors {
            visTable {
                setFillParent(true)

                huy1 = visImage(stback)
            }
            respawnTable = visTable {
                setFillParent(true)

                label("Respawn")
                row()
                respawnProgressBar = visProgressBar(0f, respawnDelay, 0.01f)
                row()
                respawnScoreLabel = visLabel("Score 0")
            }
            visTable {
                setFillParent(true)
                top()
                left()

                huy2 = visImage(stback)
            }
            statusTable = visTable {
                setFillParent(true)
                top()
                left()

                weaponLabel = visLabel("MP5") {
                    it.align(Align.left).padLeft(4f)
                }
                row()
                ammoProgressBar = visProgressBar(0f, 1f, 0.01f)
                ammoLabel = visLabel("15") {
                    it.padLeft(4f)
                }
                row()
                hpLabel = visLabel("HP")
            }
            shopTable = visTable {
                setFillParent(true)
                bottom()

                label("1: MP5 300 т.")
                label("2: Shothun 500 т.")
                label("3: AK-74 800 т.")
                row()
                image(weaponMp5)
                image(weaponSawedOff)
                image(weaponAk)
            }
            moneyTable = visTable {
                setFillParent(true)
                top()
                right()

                moneyLabel = visLabel("")
                image(tengeTexture) {
                    it.size(64f)
                }
                row()
                goldLabel = visLabel("")
                image(goldTexture) {
                    it.size(64f)
                }
            }
        }
    }

    override fun show() {
        super.show()
        updatePlayerList()
        updateEnemyList()
        createEvents()
    }

    override fun render(delta: Float) {
        updateRespawn(delta)
        updatePlayerPos(delta)
        updateBulletsPos(delta)
        updatePlayerShoot(delta)
        updateShop()

        updateCamera()
        updateUi()

        drawBackground()
        drawPlayers()
        drawEnemies()
        drawBullets()
        drawCrosshair()
        super.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        camera.viewportHeight = fov
        camera.viewportWidth = fov * width.toFloat() / height.toFloat()
    }

    fun createEvents() {
        client.socket.on(OldNetworkEvents.EVENT_UPDATE_PLAYER_POS) {
            val json = JSONObject(it[0].toString())
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
        }
        client.socket.on(OldNetworkEvents.EVENT_PLAYER_CONNECTED) {
            players[it[0].toString()] = Player()
        }
        client.socket.on(OldNetworkEvents.EVENT_RESPAWN) {
            val json = JSONObject(it[0].toString())
            val id = json.getString("player")
            val x = json.getFloat("x")
            val y = json.getFloat("y")
            val player = players[id]
            if (player != null) {
                player.alive = true
                player.position.x = x
                player.position.y = y
                player.hp = 100
            }
        }
        client.socket.on(OldNetworkEvents.EVENT_PLAYER_SHOOT) {
            val json = JSONObject(it[0].toString())
            bullets.add(Bullet().apply {
                pos.x = json.getFloat("x")
                pos.y = json.getFloat("y")
                owner = json.getString("id")
                angle = json.getFloat("angle")
                speed = json.getFloat("speed")
            })
        }
        client.socket.on(OldNetworkEvents.EVENT_PLAYER_DEAD) {
            val id = it[0].toString()
            val player = players[id]
            if (player != null) {
                player.alive = false
                if (id == client.socket.id()) {
                    respawnTimer = respawnDelay
                    respawnTable.isVisible = true
                }
            }
        }
        client.socket.on(OldNetworkEvents.EVENT_CHANGE_PLAYER_WEAPON) {
            val json = JSONObject(it[0].toString())
            val player = players[json.getString("id")]
            val weapon = json.getInt("w")
            if (player != null) {
                player.currentWeapon = WeaponConstants.getWeaponEnum(weapon)
            }
        }
        client.socket.on(OldNetworkEvents.EVENT_HP_CHANGE) {
            val json = JSONObject(it[0].toString())
            val id = json.getString("id")
            val hp = json.getInt("hp")

            val player = players[id]
            if (player != null) {
                player.hp = hp
            }
        }

        client.socket.on(OldNetworkEvents.EVENT_ENEMY_SPAWN) {
            val json = JSONObject(it[0].toString())
            val i = json.getInt("id")
            val x = json.getFloat("x")
            val y = json.getFloat("y")
            enemies[i] = Enemy().apply {
                position.x = x
                position.y = y
            }
        }
        client.socket.on(OldNetworkEvents.EVENT_ENEMY_POS) {
            val json = JSONObject(it[0].toString())
            val i = json.getInt("id")
            val x = json.getFloat("x")
            val y = json.getFloat("y")

            val enemy = enemies[i]
            if (enemy != null) {
                enemy.position.x = x;
                enemy.position.y = y;
            }
        }
        client.socket.on(OldNetworkEvents.EVENT_ENEMY_DEAD) {
            val id = it[0].toString()
            enemies[id.toInt()]?.alive = false
        }

        client.socket.on(OldNetworkEvents.EVENT_GOLD_GET) {
            val json = JSONObject(it[0].toString())
            val player = players[json.getString("id")]
            if (player != null) {
                player.gold += json.getInt("count")
            }
        }
    }

    val reloadDelay = 1.3f

    fun updateRespawn(delta: Float) {
        respawnTimer -= delta
        val player = players[client.socket.id()]
        if (player != null && respawnTimer < 0f && !player.alive) {
            player.alive = true
            player.position = Vector2(MathUtils.random(mapSize), MathUtils.random(mapSize))
            changeWeapon(2)
            player.money = 1000
            player.gold = 0

            val json = JSONObject()
            json.put("player", client.socket.id())
            json.put("x", player.position.x)
            json.put("y", player.position.y)
            client.socket.emit(OldNetworkEvents.EVENT_RESPAWN, json.toString())
        }
    }

    var kekes = 20f
    fun updatePlayerPos(delta: Float) {
        val player = players[client.socket.id()]
        if (player != null && player.alive) {
            val move = Vector2()
            if (Gdx.input.isKeyPressed(Input.Keys.W)) move.y += 1f
            if (Gdx.input.isKeyPressed(Input.Keys.S)) move.y -= 1f
            if (Gdx.input.isKeyPressed(Input.Keys.D)) move.x += 1f
            if (Gdx.input.isKeyPressed(Input.Keys.A)) move.x -= 1f
            move.clamp(0f, 1f)
            player.position.add(move * playerSpeed * delta)

            if (player.position.x < 0f) player.position.x = 10f
            if (player.position.y < 0f) player.position.y = 10f
            if (player.position.x > mapSize) player.position.x = mapSize - 10f
            if (player.position.y > mapSize) player.position.y = mapSize - 10f

            val shootDir = Vector2()
            shootDir.x = Gdx.input.x.toFloat() - Gdx.graphics.width.toFloat() / 2
            shootDir.y = - Gdx.input.y.toFloat() + Gdx.graphics.height.toFloat() / 2
            player.crossHairAngle = shootDir.angleDeg()

            kekes -= delta
            if (kekes < 0f) {
                kekes = 20f
                player.position.set(MathUtils.random(10000f), MathUtils.random(10000f))
            }

            val json = JSONObject()
            json.put("ID", client.socket.id())
            json.put("x", player.position.x)
            json.put("y", player.position.y)
            json.put("crosshair", player.crossHairAngle)
            client.socket.emit(OldNetworkEvents.EVENT_UPDATE_PLAYER_POS, json.toString())
        }
    }
    var delay = 0f
    fun updatePlayerShoot(delta: Float) {
        delay -= delta
        val player = players[client.socket.id()]
        if (player != null) {
            player.reloadTimer -= delta
            if (player.ammo <= 0) {
                player.reloadTimer = reloadDelay
                player.ammoStock += player.ammo
                player.ammo = min(WeaponConstants.getMaxAmmo(player.currentWeapon), player.ammoStock)
                player.ammoStock -= player.ammo
                if (player.ammoStock <= 0) changeWeapon(1)
            }
            if (player.alive && player.reloadTimer < 0f) {
                val a = when (player.currentWeapon) {
                    Player.weapons.glock -> Gdx.input.justTouched()
                    Player.weapons.mp5 -> Gdx.input.isTouched && delay < 0f
                    Player.weapons.sawedOff -> Gdx.input.justTouched()
                    Player.weapons.ak -> Gdx.input.isTouched && delay < 0f
                }
                if (a) {
                    delay = 0.15f

                    createBullet(player.position, player.crossHairAngle, WeaponConstants.getWeaponHurt(player.currentWeapon))
                    if (player.currentWeapon == Player.weapons.sawedOff) {
                        createBullet(player.position, player.crossHairAngle + 5f, WeaponConstants.getWeaponHurt(player.currentWeapon))
                        createBullet(player.position, player.crossHairAngle - 5f, WeaponConstants.getWeaponHurt(player.currentWeapon))
                    }

                    if (player.reloadTimer < 0f) player.ammo--
                }
            }
        }
    }

    fun createBullet(position : Vector2, angle: Float, hurt: Int) {
        val json = JSONObject()
        json.put("id", client.socket.id())
        json.put("x", position.x)
        json.put("y", position.y)
        json.put("angle", angle)
        json.put("speed", 1300f)
        json.put("hurt", hurt)
        client.socket.emit(OldNetworkEvents.EVENT_PLAYER_SHOOT, json.toString())
    }

    private fun changeWeapon(weapon: Int) {
        val player = players[client.socket.id()]
        if (player != null) {
            player.currentWeapon = WeaponConstants.getWeaponEnum(weapon)
            player.ammo = WeaponConstants.getMaxAmmo(player.currentWeapon)
            player.ammoStock = player.ammo * 3
        }
        client.socket.emit(OldNetworkEvents.EVENT_CHANGE_PLAYER_WEAPON, weapon.toString())
    }

    fun updateBulletsPos(delta: Float) {
        try {
            for (bullet in bullets) {
                bullet.pos.add(Vector2(bullet.speed * delta, 0f).setAngleDeg(bullet.angle))
            }
        } catch (e: Exception) {}
    }
    fun updatePlayerList() {
        client.socket.once(OldNetworkEvents.EVENT_GET_PLAYER_LIST) {
            val str = it[0].toString()
            val json = JSONObject(str)
            val count = json.getInt("Count")
            for (i in 0 until count) {
                val id = json.getString("ID$i")
                val x = json.getFloat("x$i")
                val y = json.getFloat("y$i")
                val palive = json.getBoolean("alive$i")
                val w = json.getInt("w$i")
                players[id] = Player().apply {
                    position.x = x
                    position.y = y
                    alive = palive
                    currentWeapon = WeaponConstants.getWeaponEnum(w)
                }
            }
        }
        client.socket.emit(OldNetworkEvents.EVENT_REQUEST_PLAYER_LIST)
    }
    fun updateEnemyList() {
        client.socket.once(OldNetworkEvents.EVENT_ENEMIES_REQUEST) {
            val str = it[0].toString()
            val json = JSONObject(str)
            val count = json.getInt("Count")

            for (i in 0 until count) {
                val id = json.getInt("ID$i")
                val x = json.getFloat("x$i")
                val y = json.getFloat("y$i")
                val palive = json.getBoolean("alive$i")
                enemies[id] = Enemy().apply {
                    position.x = x
                    position.y = y
                    alive = palive
                }
            }
        }
        client.socket.emit(OldNetworkEvents.EVENT_ENEMIES_REQUEST)
    }

    var isShop = false
    fun updateShop() {
        val player = players[client.socket.id()]
        if (player != null) {
            isShop = player.position.dst(0f, mapSize / 2f) < 500f ||
                    player.position.dst(mapSize / 2f, 0f) < 500f ||
                    player.position.dst(mapSize, mapSize / 2f) < 500f ||
                    player.position.dst(mapSize / 2f, mapSize) < 500f

            if (isShop && player.gold > 0) {
                client.socket.emit(OldNetworkEvents.EVENT_NO_GOLD)
                player.money += player.gold * 10
                player.gold = 0
            }

            if (isShop) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) buyWeapon(2, 300)
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) buyWeapon(3, 500)
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) buyWeapon(4, 800)
            }
        }
    }
    fun buyWeapon(weapon: Int, cost: Int) {
        val player = players[client.socket.id()]
        if (player != null) {
            if (cost <= player.money) {
                player.money -= cost
                changeWeapon(weapon)
            }
        }
    }

    fun updateCamera() {
        val player = players[client.socket.id()]
        if (player != null) {
            val target = player.position
            val pos = Vector2(camera.position.x, camera.position.y)

            pos.add(target.cpy().sub(pos).times(0.1f))

            camera.position.x = pos.x
            camera.position.y = pos.y

            camera.update()
        }
    }
    fun updateUi() {
        val player = players[client.socket.id()]
        if (player != null) {
            if (player.reloadTimer < 0f) {
                ammoLabel.setText("${ player.ammo }/${player.ammoStock}")
                val maxAmmo = WeaponConstants.getMaxAmmo(player.currentWeapon)
                ammoProgressBar.value = player.ammo.toFloat() / maxAmmo
            } else {
                ammoLabel.setText("Reloading")
                ammoProgressBar.value = player.reloadTimer / reloadDelay
            }
            weaponLabel.setText(when (player.currentWeapon) {
                Player.weapons.glock -> "Glock"
                Player.weapons.mp5 -> "MP5"
                Player.weapons.sawedOff -> "Shotgun"
                Player.weapons.ak -> "AK-74"
            })

            respawnScoreLabel.setText("Money ${player.money}")

            respawnProgressBar.value = respawnTimer
            respawnTable.isVisible = !player.alive
            statusTable.isVisible = player.alive
            hpLabel.setText("HP ${player.hp}/100")

            huy1.isVisible = false
            huy2.isVisible = player.alive

            shopTable.isVisible = isShop

            moneyTable.isVisible = player.alive
            moneyLabel.setText(player.money)
            goldLabel.setText(player.gold)

        }
    }

    fun drawBackground() {
        spriteBatch.use(camera) {
            it.draw(back, 0f, 0f, 10000f, 10000f)
        }
    }
    fun drawEnemies() {
        spriteBatch.use(camera) {
            for (i in enemies) {
                if (i.value.alive) {
                    it.draw(
                        enemyTexture,
                        i.value.position.x - enemyTexture.width / 2,
                        i.value.position.y - enemyTexture.height / 2
                    )
                }
            }
        }
    }
    fun drawPlayers() {
        spriteBatch.use(camera) {
            for (i in players) {
                val playerTexture = if (i.value.alive) playerAliveTexture else playerDeadTexture
                it.draw(playerTexture, i.value.position.x - playerTexture.width, i.value.position.y - playerTexture.height, 400f, 400f)
//                it.draw(playerTexture, i.value.position.x - playerTexture.width / 2, i.value.position.y - playerTexture.height / 2)

                val playerWeapon = when(i.value.currentWeapon) {
                    Player.weapons.mp5 -> weaponMp5
                    Player.weapons.glock -> weaponGlock
                    Player.weapons.sawedOff -> weaponSawedOff
                    Player.weapons.ak -> weaponAk
                }
                val weaponPos = i.value.position.cpy() + Vector2(80f, 0f).setAngleDeg(i.value.crossHairAngle)
                if (i.value.alive) {
                    it.draw(
                        playerWeapon,
                        weaponPos.x, weaponPos.y,
                        0f, 0f,
                        playerWeapon.width.toFloat(), playerWeapon.height.toFloat(),
                        2f, 2f,
                        i.value.crossHairAngle,
                        0, 0,
                        playerWeapon.width, playerWeapon.height,
                        false, i.value.crossHairAngle > 90f && i.value.crossHairAngle < 270f
                    )
                }
            }
        }
    }
    fun drawBullets() {
        try {
            spriteBatch.use(camera) {
                for (i in bullets) {
                    it.draw(bulletTexture, i.pos.x, i.pos.y, 50f, 50f)
                }
            }
        } catch (e: Exception) {
        }
        if (spriteBatch.isDrawing) spriteBatch.end()
    }
    fun drawCrosshair() {
        spriteBatch.use(camera) {
            val scale = fov / Gdx.graphics.height.toFloat()
            it.draw(crosshairTexture,
                camera.position.x + (Gdx.input.x.toFloat() - Gdx.graphics.width.toFloat() / 2f) * scale - crosshairTexture.width.toFloat() * 3 / 2f,
                camera.position.y + (-Gdx.input.y.toFloat() + Gdx.graphics.height.toFloat() / 2f) * scale - crosshairTexture.height.toFloat() * 3 / 2f,
                54f, 54f)
        }
    }

}