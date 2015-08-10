package exemel2

import scalaz._, Scalaz._

sealed trait XmlError

object XmlError {
  case class ElementNotFound(name: String) extends XmlError
  case class AttributeNotFound(name: String) extends XmlError
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

case class Zipper() {
  def attribute(name: String): Option[Zipper] = ???
  def firstElementByName(name: String): Option[Zipper] = ???
  def allElementsByName(name: String): List[Zipper] = ???
  def nthChild(n: Int): Option[Zipper] = ???
  def textContent: Option[String] = ???
}

case class Cursor(z: Zipper) extends AnyVal {
  def to[A](implicit ev: FromXML[A]): DecodeResult[A] =
    ev.fromXML(this)

  def \(name: String): DecodeResult[Cursor] = ???
  def element(name: String): DecodeResult[Cursor] = ???

  def \?(name: String): DecodeResult[Option[Cursor]] = ???
  def elementOpt(name: String): DecodeResult[Option[Cursor]] = ???

  def \*(name: String): DecodeResult[List[Cursor]] = ???
  def elements(name: String): DecodeResult[List[Cursor]] = ???

  def @@(name: String): DecodeResult[Cursor] = ???
  def attribute(name: String): DecodeResult[Cursor] = ???

  def @?(name: String): DecodeResult[Option[Cursor]] = ???
  def attributeOpt(name: String): DecodeResult[Option[Cursor]] = ???

  def ##(n: Int): DecodeResult[Cursor] = ???
  def nthChild(n: Int): DecodeResult[Cursor] = ???

  def text: DecodeResult[String] = ???
}

object syntax {
  implicit class DecodeResultTraverseOps[F[_]: Traverse](dr: DecodeResult[F[Cursor]]) {
    def eachTo[A](implicit ev: FromXML[A]): DecodeResult[F[A]] =
      dr >>= (_.traverse(_.to[A]))
  }
  implicit class DecodeResultCursorOps(dr: DecodeResult[Cursor]) {
    def to[A](implicit ev: FromXML[A]): DecodeResult[A] =
      dr >>= (_.to[A])
  }
}

trait FromXML[A] {
  def fromXML(cursor: Cursor): DecodeResult[A]
}

object FromXML {
  def apply[A](f: Cursor => DecodeResult[A]): FromXML[A] = new FromXML[A] {
    def fromXML(cursor: Cursor): DecodeResult[A] = f(cursor)
  }

  // def fromOption[A](f: Cursor => Option[A], err: String): FromXML[A] =
  //   apply(c => f(c).cata(DecodeResult.ok(_), DecodeResult.fail(err, c.z)))

  implicit val cursorFromXML: FromXML[Cursor] = apply(c => DecodeResult.ok(c))

  implicit val stringFromXML: FromXML[String] = apply(_.text)

  implicit val booleanFromXML: FromXML[Boolean] =
    apply(c => c.text >>= {
      case "true" => DecodeResult.ok(true)
      case "false" => DecodeResult.ok(false)
      case _ => DecodeResult.fail("Can't parse Boolean", c.z)
    })

  implicit val intFromXML: FromXML[Int] = apply(c =>
    c.text.map(_.parseInt.toOption) >>= (_.cata(
      DecodeResult.ok(_),
      DecodeResult.fail("Can't parse Int", c.z)
    ))
  )
}

case class XsiNil[A](opt: Option[A]) extends AnyVal

object XsiNil {
  import syntax._
  implicit def xsiNilFromXML[A: FromXML]: FromXML[XsiNil[A]] =
    FromXML(c =>
      (c @@ "xsi:nil").to[Boolean] >>= {
        case true => c.to[A].map(a => XsiNil(a.some))
        case false => DecodeResult.ok(XsiNil(none[A]))
      }
    )
}

sealed trait PhoneType
case object Fax extends PhoneType
case object Mobile extends PhoneType
case object Home extends PhoneType

object PhoneType {
  implicit val phoneTypeFromXML: FromXML[PhoneType] =
    FromXML(c => c.text >>= {
      case "Fax" => DecodeResult.ok(Fax)
      case "Mobile" => DecodeResult.ok(Mobile)
      case "Home" => DecodeResult.ok(Home)
      case _ => DecodeResult.fail("Can't decode PhoneType", c.z)
    })
}

case class Phone(
  phoneType: PhoneType,
  phone: String
)

object Phone {
  implicit val phoneFromXML: FromXML[Phone] =
    FromXML(c => (
      (c @@ "type" >>= (_.to[PhoneType])) |@|
        c.text
    )(Phone.apply _))
}

case class Contacts(
  email: String,
  phones: List[Phone]
)

object Contacts {
  implicit val contactsFromXML: FromXML[Contacts] =
    FromXML(c => (
      (c \ "email" >>= (_.to[String])) |@|
      (c \ "phones" >>= (_ \* "phone") >>=
        (_.traverse(_.to[Phone])))
    )(Contacts.apply _))
}

case class Address(
  street: String,
  building: Int,
  apartment: Option[Int],
  zip: Option[String]
)

object Address {
  implicit val addressFromXML: FromXML[Address] =
    FromXML(c => (
      (c \ "street" >>= (_.to[String])) |@|
      (c \ "building" >>= (_.to[Int])) |@|
      (c \ "building" >>= (_ @? "apartment") >>=
        (_.traverse(_.to[Int]))) |@|
      (c \? "zip" >>= (_.traverse(_.to[String])))
      // ((OptionT(c \? "building") >>= (b => OptionT(b @? "zip"))).run >>= (_.traverse(_.to[String])))
    )(Address.apply _))
}

case class Person(
  id: Int,
  firstName: String,
  lastName: String,
  contacts: Contacts,
  address: Address
)

object Person {
  implicit val personFromXML: FromXML[Person] =
    FromXML(c => (
      (c @@ "id" >>= (_.to[Int])) |@|
      (c \ "first_name" >>= (_.to[String])) |@|
      (c \ "last_name" >>= (_.to[String])) |@|
      (c \ "contacts" >>= (_.to[Contacts])) |@|
      (c \ "address" >>= (_.to[Address]))
    )(Person.apply _))
}

object WithSyntax {
  import syntax._

  implicit val phoneFromXML: FromXML[Phone] =
    FromXML(c =>
      ((c @@ "type").to[PhoneType] |@| c.text)(Phone.apply _)
    )

  implicit val contactsFromXML: FromXML[Contacts] =
    FromXML(c => (
      (c \ "email").to[String] |@|
      (c \ "phones" >>= (_ \* "phone")).eachTo[Phone]
    )(Contacts.apply _))

  implicit val addressFromXML: FromXML[Address] =
    FromXML(c => (
      (c \ "street").to[String] |@|
      (c \ "building").to[Int] |@|
      (c \ "building" >>= (_ @? "apartment")).eachTo[Int] |@|
      (c \? "zip").eachTo[String]
    )(Address.apply _))

  implicit val personFromXML: FromXML[Person] =
    FromXML(c => (
      (c @@ "id").to[Int] |@|
      (c \ "first_name").to[String] |@|
      (c \ "last_name").to[String] |@|
      (c \ "contacts").to[Contacts] |@|
      (c \ "address").to[Address]
    )(Person.apply _))
}
