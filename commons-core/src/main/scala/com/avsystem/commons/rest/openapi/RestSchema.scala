package com.avsystem.commons
package rest.openapi

import java.util.UUID

import com.avsystem.commons.misc.{NamedEnum, NamedEnumCompanion, Timestamp}
import com.avsystem.commons.rest.HttpBody

import scala.annotation.implicitNotFound

@implicitNotFound("RestSchema for ${T} not found. You may provide it by making companion object of ${T} " +
  "extend RestDataCompanion[${T}] (if it is a case class, object or sealed hierarchy). " +
  "Note that RestDataCompanion also automatically provides a GenCodec instance.")
trait RestSchema[T] { self =>
  def createSchema(resolver: SchemaResolver): RefOr[Schema]
  def name: Opt[String]

  def map[S](fun: RefOr[Schema] => Schema, newName: OptArg[String] = OptArg.Empty): RestSchema[S] =
    RestSchema.create(resolver => RefOr(fun(resolver.resolve(self))), newName)
  def named(name: String): RestSchema[T] =
    RestSchema.create(createSchema, name)
  def unnamed: RestSchema[T] =
    RestSchema.create(createSchema)
}
object RestSchema {
  def apply[T](implicit rt: RestSchema[T]): RestSchema[T] = rt

  def create[T](creator: SchemaResolver => RefOr[Schema], schemaName: OptArg[String] = OptArg.Empty): RestSchema[T] =
    new RestSchema[T] {
      def createSchema(resolver: SchemaResolver): RefOr[Schema] = creator(resolver)
      def name: Opt[String] = schemaName.toOpt
    }

  def plain[T](schema: Schema): RestSchema[T] =
    RestSchema.create(_ => RefOr(schema))

  def ref[T](refstr: String): RestSchema[T] =
    RestSchema.create(_ => RefOr.ref(refstr))

  implicit lazy val NothingSchema: RestSchema[Nothing] =
    RestSchema.create(_ => throw new NotImplementedError("RestSchema[Nothing]"))

  implicit lazy val UnitSchema: RestSchema[Unit] = plain(Schema(nullable = true))
  implicit lazy val NullSchema: RestSchema[Null] = plain(Schema(nullable = true))
  implicit lazy val VoidSchema: RestSchema[Void] = plain(Schema(nullable = true))

  implicit lazy val BooleanSchema: RestSchema[Boolean] = plain(Schema.Boolean)
  implicit lazy val CharSchema: RestSchema[Char] = plain(Schema.Char)
  implicit lazy val ByteSchema: RestSchema[Byte] = plain(Schema.Byte)
  implicit lazy val ShortSchema: RestSchema[Short] = plain(Schema.Short)
  implicit lazy val IntSchema: RestSchema[Int] = plain(Schema.Int)
  implicit lazy val LongSchema: RestSchema[Long] = plain(Schema.Long)
  implicit lazy val FloatSchema: RestSchema[Float] = plain(Schema.Float)
  implicit lazy val DoubleSchema: RestSchema[Double] = plain(Schema.Double)
  implicit lazy val BigIntSchema: RestSchema[BigInt] = plain(Schema.Integer)
  implicit lazy val BigDecimalSchema: RestSchema[BigDecimal] = plain(Schema.Number)

  implicit lazy val JBooleanSchema: RestSchema[JBoolean] = plain(Schema.Boolean.copy(nullable = true))
  implicit lazy val JCharacterSchema: RestSchema[JCharacter] = plain(Schema.Char.copy(nullable = true))
  implicit lazy val JByteSchema: RestSchema[JByte] = plain(Schema.Byte.copy(nullable = true))
  implicit lazy val JShortSchema: RestSchema[JShort] = plain(Schema.Short.copy(nullable = true))
  implicit lazy val JIntegerSchema: RestSchema[JInteger] = plain(Schema.Int.copy(nullable = true))
  implicit lazy val JLongSchema: RestSchema[JLong] = plain(Schema.Long.copy(nullable = true))
  implicit lazy val JFloatSchema: RestSchema[JFloat] = plain(Schema.Float.copy(nullable = true))
  implicit lazy val JDoubleSchema: RestSchema[JDouble] = plain(Schema.Double.copy(nullable = true))
  implicit lazy val JBigIntegerSchema: RestSchema[JBigInteger] = plain(Schema.Integer)
  implicit lazy val JBigDecimalSchema: RestSchema[JBigDecimal] = plain(Schema.Number)

  implicit lazy val TimestampSchema: RestSchema[Timestamp] = plain(Schema.DateTime)
  implicit lazy val JDateSchema: RestSchema[JDate] = plain(Schema.DateTime)
  implicit lazy val StringSchema: RestSchema[String] = plain(Schema.String)
  implicit lazy val SymbolSchema: RestSchema[Symbol] = plain(Schema.String)
  implicit lazy val UuidSchema: RestSchema[UUID] = plain(Schema.Uuid)

  implicit def arraySchema[T: RestSchema]: RestSchema[Array[T]] =
    RestSchema[T].map(Schema.arrayOf(_))
  implicit def seqSchema[C[X] <: BSeq[X], T: RestSchema]: RestSchema[C[T]] =
    RestSchema[T].map(Schema.arrayOf(_))
  implicit def setSchema[C[X] <: BSet[X], T: RestSchema]: RestSchema[C[T]] =
    RestSchema[T].map(Schema.arrayOf(_, uniqueItems = true))
  implicit def jCollectionSchema[C[X] <: JCollection[X], T: RestSchema]: RestSchema[C[T]] =
    RestSchema[T].map(Schema.arrayOf(_))
  implicit def jSetSchema[C[X] <: JSet[X], T: RestSchema]: RestSchema[C[T]] =
    RestSchema[T].map(Schema.arrayOf(_, uniqueItems = true))
  implicit def mapSchema[M[X, Y] <: BMap[X, Y], K, V: RestSchema]: RestSchema[M[K, V]] =
    RestSchema[V].map(Schema.mapOf)
  implicit def jMapSchema[M[X, Y] <: JMap[X, Y], K, V: RestSchema]: RestSchema[M[K, V]] =
    RestSchema[V].map(Schema.mapOf)

  implicit def optionSchema[T: RestSchema]: RestSchema[Option[T]] =
    RestSchema[T].map(Schema.nullable)
  implicit def optSchema[T: RestSchema]: RestSchema[Opt[T]] =
    RestSchema[T].map(Schema.nullable)
  implicit def optArgSchema[T: RestSchema]: RestSchema[OptArg[T]] =
    RestSchema[T].map(Schema.nullable)
  implicit def optRefSchema[T >: Null : RestSchema]: RestSchema[OptRef[T]] =
    RestSchema[T].map(Schema.nullable)
  implicit def nOptSchema[T: RestSchema]: RestSchema[NOpt[T]] =
    RestSchema[T].map(Schema.nullable)

  implicit def namedEnumSchema[E <: NamedEnum](implicit comp: NamedEnumCompanion[E]): RestSchema[E] =
    RestSchema.plain(Schema.enumOf(comp.values.iterator.map(_.name).toList))
  implicit def jEnumSchema[E <: Enum[E]](implicit ct: ClassTag[E]): RestSchema[E] =
    RestSchema.plain(Schema.enumOf(ct.runtimeClass.getEnumConstants.iterator.map(_.asInstanceOf[E].name).toList))
}

@implicitNotFound("RestResultType for ${T} not found. It may be provided by appropriate RestSchema or " +
  "RestResponses instance (e.g. RestSchema[T] implies RestResponses[T] which implies RestResultType[Future[T]]). " +
  "RestSchema is usually provided by making companion object of your data type extend RestDataCompanion.")
case class RestResultType[T](responses: SchemaResolver => Responses)
object RestResultType {
  implicit def forFuture[T: RestResponses]: RestResultType[Future[T]] =
    RestResultType[Future[T]](RestResponses[T].responses)
}

@implicitNotFound("RestResponses for ${T} not found. You may provide it by defining an instance of RestSchema[${T}] " +
  "which is usually done by making companion object of your data type extend RestDataCompanion")
case class RestResponses[T](responses: SchemaResolver => Responses)
object RestResponses {
  def apply[T](implicit r: RestResponses[T]): RestResponses[T] = r

  implicit val emptyResponseForUnit: RestResponses[Unit] =
    RestResponses(_ => Responses(byStatusCode = Map(
      204 -> RefOr(Response())
    )))

  implicit def fromSchema[T: RestSchema]: RestResponses[T] =
    RestResponses(resolver => Responses(byStatusCode = Map(
      200 -> RefOr(Response(content = Map(
        HttpBody.JsonType -> MediaType(schema = resolver.resolve(RestSchema[T])))
      ))
    )))
}

@implicitNotFound("RestRequestBody for ${T} not found. You may provide it by defining an instance of RestSchema[${T}] " +
  "which is usually done by making companion object of your data type extend RestDataCompanion")
trait RestRequestBody[T] {
  def requestBody(resolver: SchemaResolver, schemaAdjusters: List[SchemaAdjuster]): RefOr[RequestBody]
}
object RestRequestBody {
  def apply[T](implicit r: RestRequestBody[T]): RestRequestBody[T] = r

  def jsonRequestBody(schema: RefOr[Schema]): RequestBody =
    RequestBody(
      content = Map(
        HttpBody.JsonType -> MediaType(schema = schema)
      ),
      required = true
    )

  implicit def fromSchema[T: RestSchema]: RestRequestBody[T] =
    new RestRequestBody[T] {
      def requestBody(resolver: SchemaResolver, schemaAdjusters: List[SchemaAdjuster]): RefOr[RequestBody] =
        RefOr(jsonRequestBody(SchemaAdjuster.adjustRef(schemaAdjusters, resolver.resolve(RestSchema[T]))))
    }
}

trait SchemaResolver {
  def resolve(schema: RestSchema[_]): RefOr[Schema]
}

final class InliningResolver extends SchemaResolver {
  private[this] val resolving = new MHashSet[String]

  def resolve(schema: RestSchema[_]): RefOr[Schema] =
    try {
      schema.name.foreach { n =>
        if (!resolving.add(n)) {
          throw new IllegalArgumentException(s"Recursive schema reference: $n")
        }
      }
      schema.createSchema(this)
    }
    finally {
      schema.name.foreach(resolving.remove)
    }
}

final class SchemaRegistry(nameToRef: String => String, initial: Iterable[(String, RefOr[Schema])] = Map.empty)
  extends SchemaResolver {

  private[this] case class Entry(source: Opt[RestSchema[_]], schema: RefOr[Schema])

  private[this] val resolving = new MHashSet[String]
  private[this] val registry = new MLinkedHashMap[String, MListBuffer[Entry]]
    .setup(_ ++= initial.iterator.map { case (n, s) => (n, MListBuffer[Entry](Entry(Opt.Empty, s))) })

  def registeredSchemas: Map[String, RefOr[Schema]] =
    registry.iterator.map { case (k, entries) =>
      entries.result() match {
        case Entry(_, schema) :: Nil => (k, schema)
        case _ => throw new IllegalArgumentException(
          s"Multiple schemas named $k detected - you may want to disambiguate them using @name annotation"
        )
      }
    }.toMap

  def resolve(restSchema: RestSchema[_]): RefOr[Schema] = restSchema.name match {
    case Opt(name) =>
      if (!resolving.contains(name)) { // handling recursive schemas
        val entries = registry.getOrElseUpdate(name, new MListBuffer)
        if (!entries.exists(_.source.contains(restSchema))) {
          resolving += name
          val newSchema = try restSchema.createSchema(this) finally {
            resolving -= name
          }
          if (!entries.exists(_.schema == newSchema)) {
            entries += Entry(Opt(restSchema), newSchema)
          }
        }
      }
      RefOr.ref(nameToRef(name))
    case Opt.Empty =>
      restSchema.createSchema(this)
  }
}