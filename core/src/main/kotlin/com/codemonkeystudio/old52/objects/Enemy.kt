package com.codemonkeystudio.old52.objects

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2

class Enemy {

    private val mapSize = 10000f

    var position = Vector2(MathUtils.random(mapSize), MathUtils.random(mapSize))
    var alive = true
    var hp = 100

}