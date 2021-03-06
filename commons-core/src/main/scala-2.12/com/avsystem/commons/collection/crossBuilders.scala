package com.avsystem.commons
package collection

import scala.collection.compat._

trait CrossBuilder[-Elem, +To] extends MBuilder[Elem, To] {
  def addOne(elem: Elem): this.type
  def +=(elem: Elem): this.type = addOne(elem)
}

trait CrossFactory[-A, +C] extends Factory[A, C] {
  def fromSpecific(it: IterableOnce[A]): C
  def newBuilder: MBuilder[A, C]

  def apply(from: Nothing): MBuilder[A, C] = newBuilder
  def apply(): MBuilder[A, C] = newBuilder
}
