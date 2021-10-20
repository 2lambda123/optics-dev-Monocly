package monocle.impl

import monocle._
import monocle.internal._

private[monocle] trait OpticImpl[+Can, -S, +T, +A, -B]:
  self =>

  def andThen[Can2 >: Can, C, D](optic2: OpticImpl[Can2, A, B, C, D]): OpticImpl[Can2, S, T, C, D]

  final protected def unsupported(name: String): Nothing =
    sys.error(s"This optic does not support `$name`")

  protected[impl] def get(s: S): A =
    unsupported("get")

  protected[impl] def getOption(s: S): Option[A] =
    unsupported("getOption")

  protected[impl] def getOrModify(s: S): Either[T, A] =
    unsupported("getOrModify")

  protected[impl] def toIterator(s: S): Iterator[A] =
    unsupported("toIterator")

  protected[impl] def nonEmptyFoldMap[M: Semigroup](f: A => M)(s: S): M =
    unsupported("nonEmptyFoldMap")

  protected[impl] def nonEmptyModifyA[F[+_]: Apply](f: A => F[B])(s: S): F[T] =
    unsupported("nonEmptyModifyA")

  protected[impl] def modify(f: A => B): S => T =
    unsupported("modify")

  protected[impl] def modifyA[F[+_]: Applicative](f: A => F[B])(s: S): F[T] =
    unsupported("modifyA")

  protected[impl] def modifyF[F[+_]: Functor](f: A => F[B])(s: S): F[T] =
    unsupported("modifyF")

  protected[impl] def replace(b: B): S => T =
    unsupported("replace")

  protected[impl] def reverse: IsoImpl[B, A, T, S] =
    unsupported("reverse")

  protected[impl] def reverseGet(b: B): T =
    unsupported("reverseGet")

  object safe:
    inline def get(s: S)(using Can <:< Get): A =
      self.get(s)

    inline def getOption(s: S)(using Can <:< GetOption): Option[A] =
      self.getOption(s)

    inline def getOrModify(s: S)(using Can <:< EditOption): Either[T, A] =
      self.getOrModify(s)

    inline def toIterator(s: S)(using Can <:< GetMany): Iterator[A] =
      self.toIterator(s)

    inline def nonEmptyFoldMap[M](f: A => M)(s: S)(using Semigroup[M])(using Can <:< GetOneOrMore): M =
      self.nonEmptyFoldMap(f)(s)

    inline def nonEmptyModifyA[F[+_]](f: A => F[B])(s: S)(using Apply[F])(using
      Can <:< EditOneOrMore
    ): F[T] =
      self.nonEmptyModifyA(f)(s)

    inline def modify(f: A => B)(using Can <:< Modify): S => T =
      self.modify(f)

    inline def modifyA[F[+_]](f: A => F[B])(s: S)(using Applicative[F])(using Can <:< EditMany): F[T] =
      self.modifyA(f)(s)

    inline def modifyF[F[+_]](f: A => F[B])(s: S)(using Functor[F])(using Can <:< Edit): F[T] =
      self.modifyF(f)(s)

    inline def replace(b: B)(using Can <:< Modify): S => T =
      self.replace(b)

    inline def reverse(using Can <:< ConvertBetween): IsoImpl[B, A, T, S] =
      self.reverse

    inline def reverseGet(b: B)(using Can <:< ReverseGet): T =
      self.reverseGet(b)

end OpticImpl
