package json

import scala.collection.generic.{ CanBuildFrom, GenericCompanion }
import scala.collection.mutable.Builder
import scala.collection.immutable.VectorBuilder
import scala.collection.{ IterableLike, immutable }
import java.util.UUID

object JArray { //extends GenericCompanion[scala.collection.immutable.Iterable] {
  //def apply(values: IndexedSeq[JValue]): JArray = new JArray(values)

  //def unapply(obj: JArray): Option[IndexedSeq[JValue]] = Some(obj.values)

  lazy val empty = apply(IndexedSeq.empty)

  def newCanBuildFrom = new CanBuildFrom[TraversableOnce[JValue], JValue, JArray] {
    def apply(from: TraversableOnce[JValue]) = newJArrayBuilder // ++= from
    def apply() = newJArrayBuilder
  }

  implicit def canBuildFrom: CanBuildFrom[TraversableOnce[JValue], JValue, JArray] =
    newCanBuildFrom

  def newJArrayBuilder: Builder[JValue, JArray] = new JArrayBuilder

  def apply(seq: TraversableOnce[JValue]): JArray = new JArray(seq.toIndexedSeq)
  //def apply[T <: JValue](x: T, xN: T*): JArray = apply(x +: xN)

  class JArrayBuilder extends Builder[JValue, JArray] {
    val builder = new VectorBuilder[JValue]

    def +=(item: JValue): this.type = {
      builder += item
      this
    }

    def result: JArray = {
      JArray(builder.result)
    }

    def clear() {
      builder.clear
    }
  }

  def newBuilder[A]: Builder[A, immutable.Iterable[A]] =
    newJArrayBuilder.asInstanceOf[Builder[A, immutable.Iterable[A]]]
}

final case class JArray(override val values: IndexedSeq[JValue]) extends JValue with scala.collection.immutable.Iterable[JValue] with IterableLike[JValue, JArray] {
  lazy val uuid = UUID.randomUUID.toString

  def toJString: JString = JString("array " + uuid) //this... should be different
  def toJNumber: JNumber = JNaN
  def toJBoolean: JBoolean = JTrue
  //override def toObject

  def length = values.length

  def iterator: Iterator[JValue] = values.iterator

  def value = values.map(_.value)

  //override def companion: GenericCompanion[scala.collection.immutable.Iterable] = JArray
  override def newBuilder = JArray.newJArrayBuilder

  override def toSeq = values
  override def seq = this

  override def jValue = this
  override def toJArray: JArray = this
  //override def keys = (0 until values.length).map(JNumber(_)).toSet

  override def jObject: JObject = throw GenericJSONException("Expected JObject")
  override def jArray: JArray = this
  override def jNumber: JNumber = throw GenericJSONException("Expected JNumber")
  override def jString: JString = throw GenericJSONException("Expected JString")
  override def jBoolean: JBoolean = throw GenericJSONException("Expected JBoolean")

  override def toString = toJSONString

  def apply(key: JNumber): JValue = apply(key: JValue)

  override def apply(key: JValue): JValue = {
    val jNum = key.toJNumber
    val jInt = jNum.num.toInt

    if (jNum.num != jInt) JUndefined
    else if (jInt < 0) JUndefined
    else if (jInt >= values.length) JUndefined
    else values(jInt)
  }

  def ++(that: JArray): JArray =
    JArray(values ++ that.values)

  def toJSONStringBuilder(settings: JSONBuilderSettings,
    lvl: Int): StringBuilder = {
    val out = new StringBuilder

    out.append("[")

    var isFirst = true
    values foreach { v =>
      if (!isFirst) out.append("," + settings.spaceString)

      out append v.toJSONStringBuilder(settings)

      isFirst = false
    }

    out.append("]")
  }
}
