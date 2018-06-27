package com.wavesplatform.lang.v1.compiler

import com.wavesplatform.lang.v1.compiler.Types.TYPEPLACEHOLDER.LISTCONCRETE
import com.wavesplatform.lang.v1.evaluator.ctx.{CaseObj, DefinedType}
import scodec.bits.ByteVector

object Types {

  sealed trait TYPEPLACEHOLDER
  sealed trait CONCRETE extends TYPEPLACEHOLDER
  object TYPEPLACEHOLDER {
    case class TYPEPARAM(char: Byte)             extends TYPEPLACEHOLDER
    case class LISTTYPEPARAM(t: TYPEPLACEHOLDER) extends TYPEPLACEHOLDER //getElement
    case class LISTCONCRETE(t: CONCRETE)         extends CONCRETE
    case object UNIT                             extends CONCRETE
    case object LONG                             extends CONCRETE
    case object BYTEVECTOR                       extends CONCRETE
    case object BOOLEAN                          extends CONCRETE
    case object STRING                           extends CONCRETE
    case class CASETYPEREF(name: String)         extends CONCRETE
    case class UNION(l: List[CONCRETE])          extends CONCRETE
  }

  def typePlaceholdertoType(resultType: TYPEPLACEHOLDER,
                            resolvedPlaceholders: Map[TYPEPLACEHOLDER.TYPEPARAM, TYPE],
                            knownTypes: Map[String, DefinedType]): TYPE = {
    resultType match {
      case tp @ TYPEPLACEHOLDER.TYPEPARAM(char) => resolvedPlaceholders(tp)
      case TYPEPLACEHOLDER.LISTTYPEPARAM(t)     => LIST(typePlaceholdertoType(t, resolvedPlaceholders, knownTypes))
      case c: CONCRETE                          => concreteToType(c, knownTypes)
    }
  }

  def concreteToType(concrete: CONCRETE, knownTypes: Map[String, DefinedType]): TYPE = concrete match {
    case LISTCONCRETE(t)            => LIST(concreteToType(t, knownTypes))
    case TYPEPLACEHOLDER.UNIT       => UNIT
    case TYPEPLACEHOLDER.LONG       => LONG
    case TYPEPLACEHOLDER.BYTEVECTOR => BYTEVECTOR
    case TYPEPLACEHOLDER.BOOLEAN    => BOOLEAN
    case TYPEPLACEHOLDER.STRING     => STRING
    case TYPEPLACEHOLDER.CASETYPEREF(name) =>
      val casetyperef = knownTypes(name)
      CASETYPEREF(casetyperef.name, casetyperef.fields)
    case TYPEPLACEHOLDER.UNION(l) => UNION.create(l.flatMap(l1 => concreteToType(l1, knownTypes).l))
  }

  def typeToConcretePlaceholder(t: TYPE): CONCRETE = t match {
    case UNIT              => TYPEPLACEHOLDER.UNIT
    case LONG              => TYPEPLACEHOLDER.LONG
    case BYTEVECTOR        => TYPEPLACEHOLDER.BYTEVECTOR
    case BOOLEAN           => TYPEPLACEHOLDER.BOOLEAN
    case STRING            => TYPEPLACEHOLDER.STRING
    case LIST(lt)          => TYPEPLACEHOLDER.LISTCONCRETE(typeToConcretePlaceholder(lt))
    case UNION(l)          => TYPEPLACEHOLDER.UNION(l map typeToConcretePlaceholder)
    case CASETYPEREF(c, _) => TYPEPLACEHOLDER.CASETYPEREF(c)
  }

  sealed trait TYPE {
    type Underlying
    def name: String
    def fields: List[(String, TYPE)] = List()
    def l: List[SINGLE_TYPE]
    override def toString: String = name
  }

  sealed abstract class AUTO_TAGGED_TYPE[T](override val name: String) extends TYPE {
    override type Underlying = T
  }

  sealed trait SINGLE_TYPE extends TYPE {
    override val l = List(this)
  }

  case object NOTHING extends AUTO_TAGGED_TYPE[Nothing](name = "Nothing") {
    override val l = List()
  }
  case object UNIT       extends AUTO_TAGGED_TYPE[Unit](name = "Unit") with SINGLE_TYPE
  case object LONG       extends AUTO_TAGGED_TYPE[Long](name = "Int") with SINGLE_TYPE
  case object BYTEVECTOR extends AUTO_TAGGED_TYPE[ByteVector](name = "ByteVector") with SINGLE_TYPE
  case object BOOLEAN    extends AUTO_TAGGED_TYPE[Boolean](name = "Boolean") with SINGLE_TYPE
  case object STRING     extends AUTO_TAGGED_TYPE[String](name = "String") with SINGLE_TYPE

  case class LIST(innerType: TYPE) extends SINGLE_TYPE {
    type Underlying = IndexedSeq[innerType.Underlying]
    override lazy val name: String = "LIST(" ++ innerType.toString ++ ")"
  }
  case class CASETYPEREF(override val name: String, override val fields: List[(String, TYPE)])
      extends AUTO_TAGGED_TYPE[CaseObj](name)
      with SINGLE_TYPE

  class UNION(override val l: List[SINGLE_TYPE]) extends AUTO_TAGGED_TYPE[CaseObj](name = "UNION(" ++ l.sortBy(_.toString).mkString("|") ++ ")") {
    override lazy val fields: List[(String, TYPE)] = l.map(_.fields.toSet).reduce(_ intersect _).toList
  }

  object UNION {
    def create(l: Seq[SINGLE_TYPE]): TYPE = {
      l.flatMap {
        case UNION(l) => l
        case t        => List(t)
      } match {
        case Seq(t) => t
        case l      => new UNION(l.distinct.toList)
      }
    }
    def apply(l: SINGLE_TYPE*): TYPE                 = create(l.toList)
    def unapply(u: UNION): Option[List[SINGLE_TYPE]] = Some(u.l)

    implicit class UnionExt(l1: TYPE) {
      def equivalent(l2: TYPE): Boolean = (l1, l2) match {
        case ((l1: UNION), (l2: UNION)) => l1.l.toSet == l2.l.toSet
        case (l1, l2)                   => l1 == l2
      }

      def >=(l2: TYPE): Boolean = (l1, l2) match {
        case ((l1: UNION), (l2: UNION)) =>
          val bigger = l1.l.toSet
          l2.l.forall(bigger.contains)
        case ((l1: UNION), l2) =>
          l1.l.contains(l2)
        case (l1, l2) => l1 == l2
      }
    }
  }

  def canBeEq(type1: TYPEPLACEHOLDER.TYPEPARAM, type2: TYPEPLACEHOLDER.TYPEPARAM)(t: Map[TYPEPLACEHOLDER.TYPEPARAM, TYPE]) = ???
}
