package exemel

import scalaz._, Scalaz._

sealed abstract class Node extends Product with Serializable

object Node {
  type Name = String
  case class Text(value: String) extends Node
  case class Attribute(name: Name) extends Node
  // case class Comment(value: String) extends Node
  // case class CData(value: String) extends Node
  case class Element(name: Name, attributes: Stream[Tree[Node]]) extends Node
}

case class Crumb(
  lefts: Stream[Tree[Node]],
  node: Node,
  rights: Stream[Tree[Node]]
)

case class Zipper(
  lefts: Stream[Tree[Node]],
  focus: Tree[Node],
  rights: Stream[Tree[Node]],
  crumbs: List[Crumb]
) {
  def findFirstElementByName(n: String): Option[Zipper] = ???
  def findAllElementsByName(n: String): List[Zipper] = ???
  def findAttribute(n: String): Option[Zipper] = ???
  def nthChild(n: Int): Option[Zipper] = ???
  def children: List[Zipper] = ???
}

case class DecodeResult[A](result: (String, Zipper) \/ A) {
  def map[B](f: A => B): DecodeResult[B] =
    DecodeResult(result map f)

  def flatMap[B](f: A => DecodeResult[B]): DecodeResult[B] =
    DecodeResult(result flatMap (f(_).result))
}

object DecodeResult extends DecodeResultInstances {
  def ok[A](value: A): DecodeResult[A] =
    DecodeResult(value.right)

  def fail[A](error: String, failedZipper: Zipper): DecodeResult[A] =
    DecodeResult((error, failedZipper).left)
}

trait DecodeResultInstances {
  implicit def DecodeResultMonad: Monad[DecodeResult] = new Monad[DecodeResult] {
    def point[A](a: => A) = DecodeResult.ok(a)
    def bind[A, B](a: DecodeResult[A])(f: A => DecodeResult[B]) = a flatMap f
    override def map[A, B](a: DecodeResult[A])(f: A => B) = a map f
  }
}

case class Cursor(result: (String, Zipper) \/ Zipper) {
  def to[A](implicit ev: FromXML[A]): DecodeResult[A] = ???
  def \(name: String): DecodeResult[Cursor] = ???
  def \?(name: String): DecodeResult[Option[Cursor]] = ???
  def \*(name: String): DecodeResult[List[Cursor]] = ???
  def @@(name: String): DecodeResult[Cursor] = ???
  def @?(name: String): DecodeResult[Option[Cursor]] = ???
  def ##(n: Int): DecodeResult[Cursor] = ???
}

// case class Cursor(result: (String, Zipper) \/ Zipper) {
//   def to[A](implicit ev: FromXML[A]): DecodeResult[A] =
//     ev.fromXML(this)

//   def \(name: String): Cursor =
//     Cursor(result >>= { z =>
//       z.findFirstElementByName(name).cata(
//         _.right,
//         ("el " + name, z).left
//       )
//     })

//   def @@(name: String): Cursor =
//     Cursor(result >>= { z =>
//       z.findAttribute(name).cata(
//         _.right,
//         ("attr " + name, z).left
//       )
//     })

//   def ##(n: Int): Cursor =
//     Cursor(result >>= { z =>
//       z.nthChild(n).cata(
//         _.right,
//         ("nth " + n, z).left
//       )
//     })

//   def \*(name: String): ListCursor =
//     ListCursor(result >>= { z =>
//       z.findAllElementsByName(name).right
//     })

//   def \?(name: String): OptionCursor =
//     OptionCursor(result >>= { z =>
//       z.findFirstElementByName(name).right
//     })
// }

// case class OptionCursor(result: (String, Zipper) \/ Option[Zipper]) {
//   def to[A](implicit ev: FromXML[A]): DecodeResult[Option[A]] =
//     result.fold(
//       f => DecodeResult.fail(f._1, f._2),
//       _.cata(
//         z => ev.fromXML(Cursor(z.right)).map(_.some),
//         DecodeResult.ok(none[A])
//       )
//     )

//   def \*(name: String): ListCursor =
//     ListCursor(result >>=)

//   def \(name: String): OptionCursor =
//     OptionCursor(result >>= {
//       case Some(z) => z.findFirstElementByName(name).cata(
//         _.some.right,
//         (s"Opt \\ $name", z).left
//       )
//       case None => none.right
//     })

//   def \?(name: String): OptionCursor =
//     OptionCursor(result.map { oz =>
//       oz >>= { z =>
//         z.findFirstElementByName(name)
//       }
//     })

//   def @?(name: String): OptionCursor =
//     OptionCursor(result.map { oz =>
//       oz >>= { z =>
//         z.findAttribute(name)
//       }
//     })
// }

// case class ListCursor(result: (String, Zipper) \/ List[Zipper]) {
//   def to[A](implicit ev: FromXML[A]): DecodeResult[List[A]] =
//     result.fold(
//       f => DecodeResult.fail(f._1, f._2),
//       lz => lz.traverse(z => ev.fromXML(Cursor(z.right)))
//     )

//   def \(name: String): ListCursor =
//     ListCursor(result >>= { lz =>
//       lz.traverseU { z =>
//         z.findFirstElementByName(name).cata(
//           _.right,
//           (s"List \\ $name", z).left
//         )
//       }
//     })

//   def \*(name: String): ListCursor =
//     ListCursor(result >>= { lz =>
//       (lz >>= (_.findAllElementsByName(name))).right
//     })

//   def @@(name: String): ListCursor =
//     ListCursor(result >>= { lz =>
//       lz.traverseU { z =>
//         z.findAttribute(name).cata(
//           _.right,
//           (s"List @@ $name", z).left
//         )
//       }
//     })
// }

object syntax {
  // implicit class DecodeCursorOps[F[_]: Traverse](dr: DecodeResult[F[Cursor]]) {
  //   def to[A: FromXML]: DecodeResult[F[A]] =
  //     dr >>= (_.traverse(_.to[A]))

  //   def \(name: String): DecodeResult[F[Cursor]] = ???
  //   def \?(name: String): DecodeResult[Option[Cursor]] = ???
  //   def \*(name: String): DecodeResult[List[Cursor]] = ???
  //   def @@(name: String): DecodeResult[F[Cursor]] = ???
  //   def @?(name: String): DecodeResult[Option[Cursor]] = ???
  //   def ##(n: Int): DecodeResult[F[Cursor]] = ???
  // }

  // implicit class DecodeCursorOps(dr: DecodeResult[Option[Cursor]]) {
  //   def to[A: FromXML]: DecodeResult[A] = ???
  //   def \(name: String): DecodeResult[Cursor] = ???
  //   def \?(name: String): DecodeResult[Option[Cursor]] = ???
  //   def \*(name: String): DecodeResult[List[Cursor]] = ???
  //   def @@(name: String): DecodeResult[Cursor] = ???
  //   def @?(name: String): DecodeResult[Option[Cursor]] = ???
  //   def ##(n: Int): DecodeResult[Cursor] = ???
  // }
}

// import syntax._


trait FromXML[A] {
  def fromXML(cursor: Cursor): DecodeResult[A]
}

object FromXml {
  def apply[A](f: Cursor => DecodeResult[A]): FromXML[A] = new FromXML[A] {
    def fromXML(cursor: Cursor): DecodeResult[A] = f(cursor)
  }

  implicit def fromXMLString: FromXML[String] = ???
  implicit val fromXMLCursor: FromXML[Cursor] = apply(c => DecodeResult.ok(c))

  case class Tr(v: String)
  case class Td(a: String, b: String, c: List[String], d: Option[String], e: List[Tr])

  implicit def fromXMLTr: FromXML[Tr] = ???
  // implicit val fromXMLTd: FromXML[Td] = apply(c =>
  //   for {
  //     a <- (c \ "b" @@ "attr").to[String]
  //     b <- (c \ "b" ## 3 @@ "attr").to[String]
  //     c0 <- (c \* "a" \* "d").to[String]
  //     d <- (c \? "d" @? "attr2").to[String]
  //     e <- (c \* "d" @@ "attr3").to[Tr]
  //   } yield Td(a, b, c0, d, e)
  // )
  // implicit val fromXMLTd: FromXML[Td] = apply(c =>
  //   for {
  //     a <- (c \ "b" @@ "attr").to[String]
  //     // b <- (c \ "b").to[String]
  //     b <- (c \ "b" ## 3 @@ "attr").to[String]
  //     // c0 <- (c \* "a").to[String]
  //     c0 <- (c \* "a" \* "d").to[String]
  //     d <- (c \? "d" @? "attr2").to[String]
  //     e <- (c \* "d" @@ "attr3").to[Tr]
  //   } yield Td(a, b, c0, d, e)
  // )
  implicit val fromXMLTd: FromXML[Td] = apply(c =>
    (
      ((c \ "b") >>= (_ @@ ("attr")) >>= (_.to[String])) |@|
      ((c \ "b") >>= (_ ## 2) >>= (_.to[String])) |@|
      ((c \* "a") >>= each(_.to[String])) |@|
      ((c \? "x") >>= each(_.to[String])) |@|
      ((c \* "a") >>= each(_ @@ "attr2" >>= (_.to[Tr])))
    )(Td.apply _))

  def each[F[_]: Traverse, A](f: Cursor => DecodeResult[A])(c: F[Cursor]): DecodeResult[F[A]] =
    c.traverse(f)

  // implicit val fromXMLString: FromXML[String] = new FromXML[String] {
  //   def fromXML(cursor: Cursor): DecodeResult[String] = ???
  // }

  // implicit val fromXMLTsts: FromXML[Tst] = new FromXML[Tst] {
  //   def fromXML(cursor: Cursor): DecodeResult[Tst] =
  //     (cursor --\* "a").traverse(_.as[String]).map(Tst)
  // }
}

// sealed trait CursorFocus
// case class SingleFocus(z: Zipper) extends CursorFocus
// case class MultiFocus(zs: List[Zipper]) extends CursorFocus

// case class Cursor(result: (String, Zipper) \/ CursorFocus) {
//   def as[A](implicit ev: FromXML[A]): DecodeResult[A] = ev.fromXML(this)
//   def element(n: String): Cursor = ???
//   def elementOpt(n: String): Cursor = ???
//   def elements(n: String): Cursor = ???
//   def children: Cursor = ???
//   def attribute(n: String): Cursor = ???
//   def attributeOpt(n: String): Cursor = ???
//   def content: DecodeResult[String] = ???
//   def selected: DecodeResult[List[Cursor]] = ???
// }

// trait FromXML[A] {
//   def fromXML(cursor: Cursor): DecodeResult[A]
// }

// object FromXml {
//   def apply[A](f: Cursor => DecodeResult[A]): FromXML[A] = new FromXML[A] {
//     def fromXML(cursor: Cursor): DecodeResult[A] = f(cursor)
//   }

//   implicit def fromXMLString: FromXML[String] = apply(_.content)
//   implicit def OptionFromXML[A](implicit ev: FromXML[A]): FromXML[Option[A]] = ???
//   implicit def ListFromXML[A](implicit ev: FromXML[A]): FromXML[List[A]] =
//     apply(_.selected >>= (_.traverse(_.as[A])))

//   case class Tr(v: String)
//   case class Td(a: String, b: String, c: List[String], d: Option[String], e: List[Tr])

//   implicit val TrFromXML: FromXML[Tr] = apply(_.as[String].map(Tr))
//   implicit val TdFromXML: FromXML[Td] = apply(c =>
//     for {
//       a <- c.element("b").attribute("attr").as[String]
//       b <- c.element("b").as[String]
//       c0 <- c.elements("a").as[List[String]]
//       d <- c.elementOpt("d").attributeOpt("attr2").as[Option[String]]
//       e <- c.elements("d").attribute("attr3").as[List[Tr]]
//     } yield Td(a, b, c0, d, e)
//   )
// }
