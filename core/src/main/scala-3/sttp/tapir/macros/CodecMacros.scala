package sttp.tapir.macros

import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, SchemaAnnotations, Validator}
import sttp.tapir.internal.CodecValueClassMacro
import sttp.tapir.Mapping
import sttp.tapir.DecodeResult
import sttp.tapir.DecodeResult.Value
import sttp.tapir.Schema

trait CodecMacros {

  /** Creates a codec for an enumeration, where the validator is derived using [[sttp.tapir.Validator.derivedEnumeration]]. This requires
    * that all subtypes of the sealed hierarchy `T` must be `object`s.
    *
    * This method cannot be implicit, as there's no way to constraint the type `T` to be a sealed trait / class enumeration, so that this
    * would be invoked only when necessary.
    *
    * @tparam L
    *   The type of the low-level representation of the enum, typically a [[String]] or an [[Int]].
    * @tparam T
    *   The type of the enum.
    */
  inline def derivedEnumeration[L, T]: CreateDerivedEnumerationCodec[L, T] =
    new CreateDerivedEnumerationCodec(Validator.derivedEnumeration[T], SchemaAnnotations.derived[T])

  /** Creates a codec for an [[Enumeration]], where the validator is created using the enumeration's values. Unlike the default
    * [[derivedEnumerationValue]] method, which provides the schema implicitly, this variant allows customising how the codec is created.
    * This is useful if the low-level representation of the schema is different than a `String`, or if the enumeration's values should be
    * encoded in a different way than using `.toString`.
    *
    * Because of technical limitations of macros, the customisation arguments can't be given here directly, instead being delegated to
    * [[CreateDerivedEnumerationSchema]].
    *
    * @tparam L
    *   The type of the low-level representation of the enum, typically a [[String]] or an [[Int]].
    * @tparam T
    *   The type of the enum.
    */
  inline def derivedEnumerationValueCustomise[L, T <: scala.Enumeration#Value]: CreateDerivedEnumerationCodec[L, T] =
    new CreateDerivedEnumerationCodec(derivedEnumerationValueValidator[T], SchemaAnnotations.derived[T])

  inline given derivedStringBasedUnionEnumeration[T](using IsUnionOf[String, T]): Codec[String, T, TextPlain] =
    lazy val values = UnionDerivation.constValueUnionTuple[String, T]
    lazy val validator = Validator.enumeration(values.toList.asInstanceOf[List[T]])
    Codec.string.validate(validator.asInstanceOf[Validator[String]]).map(_.asInstanceOf[T])(_.asInstanceOf[String])

  /** A default codec for enumerations, which returns a string-based enumeration codec, using the enum's `.toString` to encode values, and
    * performing a case-insensitive search through the possible values, converted to strings using `.toString`.
    *
    * To customise the enum encoding/decoding functions, provide a custom implicit created using [[derivedEnumerationValueCustomise]].
    */
  implicit inline def derivedEnumerationValue[T <: scala.Enumeration#Value]: Codec[String, T, TextPlain] =
    derivedEnumerationValueCustomise[String, T].defaultStringBased

  private inline def derivedEnumerationValueValidator[T <: Enumeration#Value]: Validator.Enumeration[T] = ${
    SchemaCompanionMacros.derivedEnumerationValueValidator[T]
  }

  /** Creates a codec for value class based on codecs defined in `Codec` companion */
  implicit inline def derivedValueClass[T <: AnyVal]: Codec[String, T, TextPlain] = CodecValueClassMacro.derivedValueClass[T]
}
