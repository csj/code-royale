package com.codingame.game

import com.codingame.gameengine.core.AbstractPlayer

class Player : AbstractPlayer() {
    override fun getExpectedOutputLines(): Int = 3
    lateinit var kingUnit: OwnedUnit
    lateinit var engineerUnit: OwnedUnit
    lateinit var generalUnit: OwnedUnit
    fun allUnits() = mainUnits + subUnits
    val mainUnits by lazy { listOf(kingUnit, engineerUnit, generalUnit) }
    val subUnits = mutableListOf<MyUnit>()
    var health : Int = MAX_HEALTH
    var resources: Int = 0
    lateinit var enemyPlayer: Player
}