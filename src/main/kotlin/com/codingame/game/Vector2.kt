package com.codingame.game

import java.lang.IllegalArgumentException
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Distance(private val squareDistance: Double): Comparable<Distance> {
  override fun compareTo(other: Distance) = squareDistance.compareTo(other.squareDistance)
  operator fun compareTo(compareDist: Double) = squareDistance.compareTo(compareDist * compareDist)
  operator fun compareTo(compareDist: Int) = squareDistance.compareTo(compareDist * compareDist)
  val toDouble by lazy { sqrt(squareDistance) }
}

@Suppress("MemberVisibilityCanBePrivate", "unused")  // It's a utility, ok
data class Vector2(val x: Double, val y: Double) {
  private val lengthSquared by lazy { x*x + y*y }
  val length by lazy { Distance(lengthSquared) }
  val isNearZero by lazy { Math.abs(x) < 1e-12 && Math.abs(y) < 1e-12 }
  val normalized: Vector2 by lazy {
    val len = length
    when {
      len < 1e-6 -> Vector2(1,0)
      else -> Vector2(x / len.toDouble, y / len.toDouble)
    }
  }
  val angle by lazy { Math.atan2(y, x) }

  constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())

  operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
  operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
  operator fun times(other: Double) = Vector2(x * other, y * other)
  operator fun div(other: Double) = when(other) {
    0.0 -> throw IllegalArgumentException("Division by zero")
    else -> Vector2(x / other, y / other)
  }
  operator fun unaryMinus() = Vector2(-x, -y)
  fun resizedTo(newLength: Double) = normalized * newLength
  fun distanceTo(other: Vector2) = (this - other).length
  fun dot(other: Vector2) = x * other.x + y * other.y
  fun compareDirection(other: Vector2) = normalized.dot(other.normalized)   /* 1 == same direction, -1 == opposite direction */
  fun projectInDirectionOf(other: Vector2) = when {
    other.isNearZero -> throw IllegalArgumentException("cannot project in direction of zero")
    else -> other * (this.dot(other) / other.dot(other))
  }
  fun rejectInDirectionOf(other: Vector2) = this - projectInDirectionOf(other)
  fun towards(other: Vector2, maxDistance: Double) =
    if (distanceTo(other) < maxDistance) other
    else this + (other - this).resizedTo(maxDistance)
  fun clampWithin(minX: Double, maxX: Double, minY: Double, maxY: Double): Vector2 {
    val nx = when { x < minX -> minX; x > maxX -> maxX; else -> x }
    val ny = when { y < minY -> minY; y > maxY -> maxY; else -> y }
    return Vector2(nx, ny)
  }
  override fun toString(): String = "(${Math.round(x)}, ${Math.round(y)})"
  fun snapToIntegers(): Vector2 = Vector2(x.roundToInt(), y.roundToInt())

  companion object {
    val Zero = Vector2(0, 0)
    fun random(theRandom: Random, maxX: Int, maxY: Int) = Vector2(theRandom.nextInt(maxX), theRandom.nextInt(maxY))
  }
}
