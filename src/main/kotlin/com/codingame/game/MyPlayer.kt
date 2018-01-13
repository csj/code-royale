package com.codingame.game

import com.codingame.gameengine.core.AbstractPlayer

class MyPlayer : AbstractPlayer() {
    override fun getExpectedOutputLines(): Int = 3
    lateinit var kingUnit: OwnedUnit
    lateinit var engineerUnit: OwnedUnit
    lateinit var generalUnit: OwnedUnit
    fun allUnits() = listOf(kingUnit, engineerUnit, generalUnit)
}