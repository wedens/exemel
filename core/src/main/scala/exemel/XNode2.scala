package exemel2

import scalaz._, Scalaz._

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
  def to[A](implicit ev: FromXML[A]): DecodeResult[A] = ???
  def \(name: String): DecodeResult[Cursor] = ???
  def \?(name: String): DecodeResult[Option[Cursor]] = ???
  def \*(name: String): DecodeResult[List[Cursor]] = ???
  def @@(name: String): DecodeResult[Cursor] = ???
  def @?(name: String): DecodeResult[Option[Cursor]] = ???
  def ##(n: Int): DecodeResult[Cursor] = ???
}

trait FromXML[A] {
  def fromXML(cursor: Cursor): DecodeResult[A]
}

object FromXML {
  def apply[A](f: Cursor => DecodeResult[A]): FromXML[A] = new FromXML[A] {
    def fromXML(cursor: Cursor): DecodeResult[A] = f(cursor)
  }

  implicit val cursorFromXML: FromXML[Cursor] = apply(c => DecodeResult.ok(c))
}
