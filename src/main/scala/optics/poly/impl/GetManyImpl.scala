package optics.poly


class GetManyImpl[+ThisCan <: GetMany, -S,+A](val getAll: S => List[A]) extends GetterImpl[ThisCan, S, A]: 

  override def preComposeGetMany[ThatCan <: GetMany, S0](impl1: GetManyImpl[ThatCan, S0,S]): GetManyImpl[ThisCan | ThatCan, S0, A] = 
    GetManyImpl(s0 => impl1.getAll(s0).flatMap(getAll))

  // override def preComposeGetOneOrMore[AllowedByBoth >: (GetMany | GetOneOrMore) <: OpticCan, S0](impl1: GetOneOrMoreImpl[S0,S]): GetManyImpl[S0, A] =
  //   GetManyImpl(s0 => impl1.getOneOrMore(s0).toList.flatMap(getAll))

  override def preComposeGetOption[ThatCan <: GetOption, S0](impl1: GetOptionImpl[ThatCan, S0,S]): GetManyImpl[ThisCan | ThatCan, S0, A] = 
    GetManyImpl(s0 => impl1.getAll(s0).flatMap(getAll))

  override def preComposeGetOne[ThatCan <: GetOne, S0](impl1: GetOneImpl[ThatCan, S0,S]): GetManyImpl[ThisCan | ThatCan, S0, A] = 
    GetManyImpl(s0 => getAll(impl1.get(s0)))

  override def andThen[ThatCan <: OpticCan, C](impl2: GetterImpl[ThatCan, A, C]): GetterImpl[ThisCan | ThatCan, S, C] = 
    impl2.preComposeGetMany(this)

  override def doGetAll(using ThisCan <:< GetMany): S => List[A] = getAll

end GetManyImpl