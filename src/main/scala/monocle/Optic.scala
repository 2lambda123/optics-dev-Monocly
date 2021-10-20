package monocle

import monocle.impl._
import monocle.functions.*
import monocle.internal.*

type Optic[+Can, S, A]      = POptic[Can, S, S, A, A]
type OpticGet[+Can, -S, +A] = POptic[Can, S, Nothing, A, Any]

object OpticGet:
  def id[A]: OpticGet[Get, A, A] =
    Optic.thatCan.get[A, A](identity)

object Optic:
  def id[A]: Optic[ConvertBetween, A, A] =
    POptic.id

  object thatCan extends OpticConstructors(POptic.thatCan)

end Optic

object POptic:
  def id[A, B]: POptic[ConvertBetween, A, B, A, B] =
    thatCan.convertBetween[A, B, A, B](identity)(identity)

  object thatCan extends POpticConstructors

end POptic

final class POptic[+Can, -S, +T, +A, -B] private[monocle] (
  protected[monocle] val impl: OpticImpl[Can, S, T, A, B]
):

  def andThen[Can2 >: Can, C, D](optic2: POptic[Can2, A, B, C, D]): POptic[Can2, S, T, C, D] =
    POptic(impl.andThen(optic2.impl))

  def all(p: A => Boolean)(using Can <:< GetMany): S => Boolean =
    impl.safe.toIterator(_).forall(p)

  def choice[S1 >: T <: S, A1 >: A <: B, S2](other: Optic[Get, S2, A1])(using Can <:< Get): Optic[Get, Either[S1, S2], A1] =
    Optic.thatCan.get(_.fold(this.get, other.get))

  def each[S1 >: T <: S, A1 >: A <: B, C](using evEach: Each[A1, C]): Optic[Can | EditMany, S1, C] =
    andThen(evEach.each)

  def exist(p: A => Boolean)(using Can <:< GetMany): S => Boolean =
    impl.safe.toIterator(_).exists(p)

  def find(p: A => Boolean)(using Can <:< GetMany): S => Option[A] =
    impl.safe.toIterator(_).find(p)

  def fold[A1 >: A : Monoid](s: S)(using Can <:< GetMany): A1 = 
    this.foldMap[A1](identity)(s)

  def foldMap[M: Monoid](f: A => M)(s: S)(using Can <:< GetMany): M = {
    var result: M = Monoid[M].empty
    val it        = impl.safe.toIterator(s)
    while (it.hasNext) result = Monoid[M].combine(result, f(it.next()))
    result
  }

  def get(s: S)(using Can <:< Get): A =
    impl.safe.get(s)

  def getAll(s: S)(using Can <:< GetMany): List[A] =
    impl.safe.toIterator(s).toList

  def getOneOrMore(s: S)(using Can <:< GetOneOrMore): NonEmptyList[A] =
    impl.safe.nonEmptyFoldMap[NonEmptyList[A]](NonEmptyList(_, Nil))(s)

  def getOption(s: S)(using Can <:< GetOption): Option[A] =
    impl.safe.getOption(s)

  def getOrModify(s: S)(using Can <:< EditOption): Either[T, A] =
    impl.safe.getOrModify(s)

  def head(s: S)(using Can <:< GetOneOrMore): A = {
    val it = impl.safe.toIterator(s)
    it.next()
  }

  def headOption(s: S)(using Can <:< GetMany): Option[A] = {
    val it = impl.safe.toIterator(s)
    if (it.hasNext) Some(it.next()) else None
  }

  def index[S1 >: T <: S, A1 >: A <: B, I, V](i: I)(using evIndex: Index[A1, I, V]): Optic[Can | EditOption, S1, V] =
    andThen(evIndex.index(i))

  def isEmpty(s: S)(using Can <:< GetMany): Boolean =
    impl.safe.toIterator(s).isEmpty

  def lastOption(s: S)(using Can <:< GetMany): Option[A] = {
    var result: Option[A] = None
    for (value <- impl.safe.toIterator(s)) result = Some(value)
    result
  }

  def length(s: S)(using Can <:< GetMany): Int =
    impl.safe.toIterator(s).length

  def mapping[F[+_]: Functor](using Can <:< ConvertBetween): POptic[ConvertBetween, F[S], F[T], F[A], F[B]] =
    POptic.thatCan.convertBetween[F[S], F[T], F[A], F[B]](fs => Functor[F].map(fs)(get))(fb =>
      Functor[F].map(fb)(this.reverseGet)
    )

  def modify(f: A => B)(using Can <:< Modify): S => T =
    impl.safe.modify(f)

  def modifyA[F[+_]](f: A => F[B])(s: S)(using Applicative[F])(using Can <:< EditMany): F[T] =
    impl.safe.modifyA(f)(s)

  def modifyF[F[+_]: Functor](f: A => F[B])(s: S)(using Can <:< Edit): F[T] =
    impl.safe.modifyF(f)(s)

  def modifyOption(f: A => B)(using Can <:< EditOption): S => Option[T] =
    s => impl.safe.getOption(s).map(a => impl.safe.replace(f(a))(s))

  def nonEmpty(s: S)(using Can <:< GetMany): Boolean =
    !isEmpty(s)

  def nonEmptyFoldMap[M: Semigroup](f: A => M)(s: S)(using Can <:< GetOneOrMore): M =
    impl.safe.nonEmptyFoldMap(f)(s)

  def nonEmptyModifyA[F[+_]](f: A => F[B])(s: S)(using Apply[F])(using Can <:< EditOneOrMore): F[T] =
    impl.safe.nonEmptyModifyA(f)(s)
  
  def orElse[S1 <: S, T1 >: T, A1 >: A, B1 <: B](other: POptic[EditOption, S1, T1, A1, B1])(using Can <:< EditOption): POptic[EditOption, S1, T1, A1, B1] =
    POptic.thatCan.editOption[S1, T1, A1, B1](s => this.getOrModify(s).orElse(other.getOrModify(s)))(b =>
      s => this.replaceOption(b)(s).getOrElse(other.replace(b)(s))
    )

  def parModifyF[G[+_]](f: A => G[B])(s: S)(using p: Parallel[G])(using Can <:< EditMany): G[T] =
    p.sequential(this.modifyA(a => p.parallel(f(a)))(s)(using p.applicative))

  def parNonEmptyModifyF[F[+_]](f: A => F[B])(s: S)(using p: NonEmptyParallel[F])(using Can <:< EditOneOrMore): F[T] =
    p.sequential(this.nonEmptyModifyA(a => p.parallel(f(a)))(s)(using p.apply))

  def re[T1 >: T, B1 <: B](using Can <:< ReverseGet): Optic[Get, B1, T1] =
    Optic.thatCan.get(this.reverseGet)

  def replace(b: B)(using Can <:< Modify): S => T =
    impl.safe.modify(_ => b)

  def replaceOption(b: B)(using Can <:< EditOption): S => Option[T] =
    this.modifyOption(_ => b)

  def reverseGet(b: B)(using Can <:< ReverseGet): T =
    impl.safe.reverseGet(b)

  def some[A1, B1](using A <:< Option[A1], Option[B1] <:< B): POptic[Can | GetOption, S, T, A1, B1] =
    asInstanceOf[POptic[Can, S, T, Option[A1], Option[B1]]].andThen(std.option.pSome)

  def split[UnifiedS >: T <: S, UnifiedA >: A <: B, OtherS, OtherA](other: Optic[Get, OtherS, OtherA])(using Can <:< Get): Optic[Get, (UnifiedS, OtherS), (UnifiedA, OtherA)] =
    Optic.thatCan.get((s, s1) => (this.get(s), other.get(s1)))

  def to[C](f: A => C)(using Can <:< GetMany): POptic[Can | Get, S, Any, C, Nothing] =
    this.andThen(Optic.thatCan.get(f))

  override def toString() =
    s"POptic(${impl})"

  def withDefault[A1 >: A, B1 <: B](defaultValue: A1)(using A <:< Option[A1]): POptic[Can | ConvertBetween, S, T, A1, B1] = 
    val iso = POptic.thatCan.convertBetween[Option[A1], Option[B1], A1, B1](_.getOrElse(defaultValue))(Some.apply)
    asInstanceOf[POptic[Can, S, T, Option[A1], Option[B1]]].andThen(iso)

  def zip[UnifiedS >: T <: S, UnifiedA >: A <: B, OtherA](other: Optic[Get, UnifiedS, OtherA])(using Can <:< Get): Optic[Get, UnifiedS, (UnifiedA, OtherA)] =
    Optic.thatCan.get(s => (this.get(s), other.get(s)))

end POptic















// Optional overrides: isEmpty, nonEmpty, find, exist, all
// Lens overrides: modifyA, foldMap, find, exist
// Prism overrides: modifyA, modify, replace
// Iso overrides: foldMap, find, exist, modifyF, modifyA, modify, replace

// Fold:
// def select[A](p: A => Boolean): Fold[A, A] =
//   new Fold[A, A] {
//     def foldMap[M: Monoid](f: A => M)(s: A): M =
//       if (p(s)) f(s) else Monoid[M].empty
//   }
// def fromFoldable[F[_]: Foldable, A]: Fold[F[A], A] =
//     new Fold[F[A], A] {
//       def foldMap[M: Monoid](f: A => M)(s: F[A]): M =
//         Foldable[F].foldMap(s)(f)
//     }

// PSetter:
// def fromFunctor[F[_], A, B](implicit F: Functor[F]): PSetter[F[A], F[B], A, B] =
//     PSetter[F[A], F[B], A, B](f => F.map(_)(f))

//   /** create a [[PSetter]] from a Contravariant functor */
//   def fromContravariant[F[_], A, B](implicit F: Contravariant[F]): PSetter[F[B], F[A], A, B] =
//     PSetter[F[B], F[A], A, B](f => F.contramap(_)(f))

//   /** create a [[PSetter]] from a Profunctor */
//   def fromProfunctor[P[_, _], A, B, C](implicit P: Profunctor[P]): PSetter[P[B, C], P[A, C], A, B] =
//     PSetter[P[B, C], P[A, C], A, B](f => P.lmap(_)(f))

// PTraversal:
//   def apply3[S, T, A, B](get1: S => A, get2: S => A, get3: S => A)(_set: (B, B, B, S) => T): PTraversal[S, T, A, B] =
//   new PTraversal[S, T, A, B] {
//     def modifyA[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
//       Applicative[F].map3(f(get1(s)), f(get2(s)), f(get3(s)))(_set(_, _, _, s))
//   }

// def apply4[S, T, A, B](get1: S => A, get2: S => A, get3: S => A, get4: S => A)(
//   _set: (B, B, B, B, S) => T
// ): PTraversal[S, T, A, B] =
//   new PTraversal[S, T, A, B] {
//     def modifyA[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
//       Applicative[F].map4(f(get1(s)), f(get2(s)), f(get3(s)), f(get4(s)))(_set(_, _, _, _, s))
//   }

// def apply5[S, T, A, B](get1: S => A, get2: S => A, get3: S => A, get4: S => A, get5: S => A)(
//   _set: (B, B, B, B, B, S) => T
// ): PTraversal[S, T, A, B] =
//   new PTraversal[S, T, A, B] {
//     def modifyA[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
//       Applicative[F].map5(f(get1(s)), f(get2(s)), f(get3(s)), f(get4(s)), f(get5(s)))(_set(_, _, _, _, _, s))
//   }

// def apply6[S, T, A, B](get1: S => A, get2: S => A, get3: S => A, get4: S => A, get5: S => A, get6: S => A)(
//   _set: (B, B, B, B, B, B, S) => T
// ): PTraversal[S, T, A, B] =
//   new PTraversal[S, T, A, B] {
//     def modifyA[F[_]: Applicative](f: A => F[B])(s: S): F[T] =
//       Applicative[F].map6(f(get1(s)), f(get2(s)), f(get3(s)), f(get4(s)), f(get5(s)), f(get6(s)))(
//         _set(_, _, _, _, _, _, s)
//       )
//   }

// Traversal:
/** Merge multiple Optionals together. All Optional must target different piece of data otherwise the Traversal doesn't
  * respect all properties. See this thread for more details:
  * https://github.com/julien-truffaut/Monocle/issues/379#issuecomment-236374838.
  */
// def applyN[S, A](xs: Optional[S, A]*): Traversal[S, A] =
//   new PTraversal[S, S, A, A] {
//     def modifyA[F[_]: Applicative](f: A => F[A])(s: S): F[S] =
//       xs.foldLeft(Applicative[F].pure(s))((fs, lens) =>
//         Applicative[F].map2(lens.getOption(s).traverse(f), fs) {
//           case (None, s)    => s
//           case (Some(a), s) => lens.replace(a)(s)
//         }
//       )
//   }

// Optional:
// def void[S, A]: Optional[S, A] =
//   Optional[S, A](_ => None)(_ => identity)
// def filter[A](predicate: A => Boolean): Optional[A, A] =
//     Optional[A, A](value => if (predicate(value)) Some(value) else None)(newValue =>
//       current => if (predicate(current)) newValue else current
//     )

//PLens:
// def codiagonal[S, T]: PLens[Either[S, S], Either[T, T], S, T] =
//   PLens[Either[S, S], Either[T, T], S, T](
//     _.fold(identity, identity)
//   )(t => _.bimap(_ => t, _ => t))

// Lens:
// def codiagonal[S]: Lens[Either[S, S], S] =
//   PLens.codiagonal

// Prism:
//   /** Create a Prism using a partial function rather than Option. */
// def partial[S, A](get: PartialFunction[S, A])(reverseGet: A => S): Prism[S, A] =
//   Prism[S, A](get.lift)(reverseGet)

// /** a [[Prism]] that checks for equality with a given value */
// def only[A](a: A)(implicit A: Eq[A]): Prism[A, Unit] =
//   Prism[A, Unit](a2 => if (A.eqv(a, a2)) Some(()) else None)(_ => a)

// Iso:
// def involuted[A](update: A => A): Iso[A, A] =
// Iso(update)(update)
