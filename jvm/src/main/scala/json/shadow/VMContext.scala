/*
 * Copyright 2016 MediaMath, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package json.shadow

import json._
import json.internal.DefaultVMContext.PrimitiveArray
import json.internal.{SimpleStringBuilder, BaseVMContext, JValueObjectDeserializer}

import scala.collection.immutable.StringOps
import scala.reflect.ClassTag

object VMContext extends BaseVMContext {
  def newVMStringBuilder: SimpleStringBuilder = new SimpleStringBuilder {
    val builder = new StringBuilder(128)

    def append(str: String): internal.SimpleStringBuilder = {
      builder append str
      this
    }

    def append(char: Char): SimpleStringBuilder = {
      builder.append(char)
      this
    }

    def ensureCapacity(cap: Int): Unit = builder.ensureCapacity(cap)

    def result(): String = builder.result()
  }

  val localMapper = new ThreadLocal[JValueObjectDeserializer] {
    override protected def initialValue: JValueObjectDeserializer =
      new JValueObjectDeserializer
  }

  //TODO: do these need to be specialized?
  def createPrimitiveArray[@specialized T: ClassTag](length: Int): PrimitiveArray[T] =
    wrapPrimitiveArray(new Array[T](length))

  def wrapPrimitiveArray[@specialized T: ClassTag](from: Array[T]): PrimitiveArray[T] = new PrimitiveArray[T] {
    def length: Int = from.length

    def update(idx: Int, value: T): Unit = from(idx) = value

    def apply(idx: Int): T = from(idx)

    //for direct wrapping if/when available
    def toIndexedSeq: IndexedSeq[T] = from

    def underlying = from
  }

  def fromString(str: String): JValue = {
    val deser = localMapper.get

    //may return null, wrap in Option
    val res = Option(deser.mapper.readValue[JValue](str, classOf[JValue]))

    deser.reset()

    res getOrElse JUndefined
  }

  def fromAny(value: Any): JValue = JValue.fromAnyInternal(value)

  //modified some escaping for '/'
  final def quoteJSONString(string: String, sb: SimpleStringBuilder): SimpleStringBuilder = {
    require(string != null)

    sb.ensureCapacity(string.length)

    sb.append('"')
    for (i <- 0 until string.length) {
      string.charAt(i) match {
        case c @ ('"' | '\\') =>
          sb.append('\\')
          sb.append(c)
        //not needed?
        /*case c if c == '/' =>
					//                if (b == '<') {
					sb.append('\\')
					//                }
					sb.append(c)*/
        case '\b' => sb.append("\\b")
        case '\t' => sb.append("\\t")
        case '\n' => sb.append("\\n")
        case '\f' => sb.append("\\f")
        case '\r' => sb.append("\\r")
        case c if c < ' ' =>
          val t = "000" + Integer.toHexString(c)
          sb.append("\\u" + t.substring(t.length() - 4))
        case c => sb.append(c)
      }
    }
    sb.append('"')

    sb
  }
}

