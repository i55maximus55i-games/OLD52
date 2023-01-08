package com.codemonkeystudio.old52.objects

import com.badlogic.gdx.math.Vector2

class Player {

    var alive = false
    var hp = 100
    var position = Vector2()
    var crossHairAngle = 0f

    var reloadTimer = 0f
    var ammo = 15
    var ammoStock = ammo * 3
    var currentWeapon = weapons.mp5

    var gold = 0
    var money = 0

    enum class weapons { glock, mp5, sawedOff, ak }

}