package com.avsystem.commons
package serialization.json

import java.math.MathContext

sealed trait JsonBinaryFormat
object JsonBinaryFormat {
  case object ByteArray extends JsonBinaryFormat
  case object HexString extends JsonBinaryFormat
}

sealed trait JsonDateFormat
object JsonDateFormat {
  case object IsoInstant extends JsonDateFormat
  case object EpochMillis extends JsonDateFormat
}

case class JsonOptions(
  indentSize: OptArg[Int] = OptArg.Empty,
  asciiOutput: Boolean = false,
  bigNumbers: Boolean = true,
  mathContext: MathContext = BigDecimal.defaultMathContext,
  dateFormat: JsonDateFormat = JsonDateFormat.IsoInstant,
  binaryFormat: JsonBinaryFormat = JsonBinaryFormat.ByteArray
)
object JsonOptions {
  final val Default = JsonOptions()
}
