package com.codemonkeystudio.old52.network

class OldNetworkEvents {
    companion object {
        const val EVENT_RESPAWN = "Respawn"
        const val EVENT_UPDATE_PLAYER_POS = "Pos"
        const val EVENT_PLAYER_SHOOT = "Shoot"
        const val EVENT_PLAYER_DEAD = "Dead"
        const val EVENT_CHANGE_PLAYER_WEAPON = "WeaponSw"
        const val EVENT_HP_CHANGE = "HP"

        const val EVENT_ENEMY_SPAWN = "ESPA"
        const val EVENT_ENEMY_DEAD = "Eded"
        const val EVENT_ENEMY_POS = "Epos"
        const val EVENT_ENEMIES_REQUEST = "Ereq"

        const val EVENT_PLAYER_CONNECTED = "PlayerConnected"
        const val EVENT_REQUEST_PLAYER_LIST = "RequestPlayers"
        const val EVENT_GET_PLAYER_LIST = "GetPlayers"

        const val EVENT_GOLD_GET = "MoreGold"
        const val EVENT_NO_GOLD = "NoGold"
    }
}
