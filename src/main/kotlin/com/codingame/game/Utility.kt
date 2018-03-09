package com.codingame.game

import kotlin.reflect.KProperty

interface PropertyDelegate<in S, T> {
  operator fun getValue(source: S, property: KProperty<*>): T
  operator fun setValue(source: S, property: KProperty<*>, value: T): Unit
}

class ClampingPropertyDelegate<in S, T : Comparable<T>>(initialValue: T, private val minValue: T? = null, private val maxValue: T? = null) : PropertyDelegate<S,T>{
  var field: T = initialValue

  override operator fun getValue(source: S, property: KProperty<*>): T = field

  override operator fun setValue(source: S, property: KProperty<*>, value: T) {
    field = when {
      minValue != null && value < minValue -> minValue
      maxValue != null && value > maxValue -> maxValue
      else -> value
    }
  }
}

fun <S> nonNegative(initialValue: Int) = ClampingPropertyDelegate<S,Int>(initialValue, minValue = 0)
fun <S> nonNegativeCeiling(initialValue: Int, max: Int) = ClampingPropertyDelegate<S,Int>(initialValue, minValue = 0, maxValue = max)

fun <S,T> PropertyDelegate<S,T>.andAlso(cont: (T) -> Unit): PropertyDelegate<S,T> {
  val that = this
  return object : PropertyDelegate<S,T> {
    override fun getValue(source: S, property: KProperty<*>): T {
      return that.getValue(source, property)
    }
    override fun setValue(source: S, property: KProperty<*>, value: T) {
      that.setValue(source, property, value)
      cont(value)
    }
  }
}

fun IntRange.sample(): Int {
  return theRandom.nextInt(last-first) + first
}