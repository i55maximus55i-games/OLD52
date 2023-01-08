package com.codemonkeystudio.old52.objects

class WeaponConstants {

    companion object {
        fun getMaxAmmo(weapon: Player.weapons): Int {
            return when (weapon) {
                Player.weapons.glock -> 7
                Player.weapons.mp5 -> 15
                Player.weapons.sawedOff -> 5
                Player.weapons.ak -> 30
            }
        }

        fun getWeaponEnum(weapon: Int): Player.weapons {
            return when (weapon) {
                1 -> Player.weapons.glock
                2 -> Player.weapons.mp5
                3 -> Player.weapons.sawedOff
                4 -> Player.weapons.ak
                else -> Player.weapons.glock
            }
        }

        fun getWeaponInt(weapon: Player.weapons): Int {
            return when (weapon) {
                Player.weapons.glock -> 1
                Player.weapons.mp5 -> 2
                Player.weapons.sawedOff -> 3
                Player.weapons.ak -> 4
            }
        }

        fun getWeaponHurt(weapon: Player.weapons): Int {
            return when (weapon) {
                Player.weapons.glock -> 30
                Player.weapons.mp5 -> 20
                Player.weapons.sawedOff -> 50
                Player.weapons.ak -> 40
            }
        }
    }

}