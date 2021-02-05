package optics.internal.focus.features.optionsome

import optics.internal.focus.FocusBase

private[focus] trait OptionSomeParser {
  this: FocusBase => 

  import macroContext.reflect._

  object OptionSome extends FocusParser {

    def unapply(term: Term): Option[FocusResult[(Term, FocusAction)]] = term match {
      case Apply(TypeApply(Ident("some"), List(typeArg)), List(remainingCode)) => 
        val toType = typeArg.tpe
        val action = FocusAction.OptionSome(toType)
        Some(Right(remainingCode, action))
        
      case _ => None
    }
  }
}