package com.codingame.game

import kotlin.reflect.KProperty

class ClampingPropertyDelegate<in S, T : Comparable<T>>(initialValue: T, private val minValue: T? = null, private val maxValue: T? = null) {
  var field: T = initialValue

  operator fun getValue(source: S, property: KProperty<*>): T = field

  operator fun setValue(source: S, property: KProperty<*>, value: T) {
    field = when {
      minValue != null && value < minValue -> minValue
      maxValue != null && value > maxValue -> maxValue
      else -> value
    }
  }
}

fun <S> nonNegative(initialValue: Int) = ClampingPropertyDelegate<S,Int>(initialValue, minValue = 0)

fun IntRange.sample(): Int {
  return theRandom.nextInt(last-first) + first
}