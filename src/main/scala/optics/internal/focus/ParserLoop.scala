package optics.internal.focus

import scala.quoted.Type
import optics.internal.focus.features.fieldselect.FieldSelectParser
import optics.internal.focus.features.optionsome.OptionSomeParser

private[focus] trait AllParsers 
  extends FocusBase
  with FieldSelectParser 
  with OptionSomeParser

private[focus] trait ParserLoop {
  this: FocusBase with AllParsers => 

  import macroContext.reflect._
  
  def parseLambda[From: Type](lambda: Term): ParseResult = {
    val fromTypeIsConcrete = TypeRepr.of[From].classSymbol.isDefined

    lambda match {
      case ExpectedLambdaFunction(params) if fromTypeIsConcrete => parseLambdaBody(params)
      case ExpectedLambdaFunction(_) => FocusError.NotASimpleLambdaFunction.asResult
      case _ => FocusError.NotAConcreteClass(Type.show[Type[From]]).asResult
    }
  }

  private case class ParseParams(argName: String, argType: TypeRepr, lambdaBody: Term)

  private object LambdaArgument {
    def unapply(term: Term): Option[String] = term match {
      case Ident(idName) => Some(idName)
      case _ => None
    }
  }

  private def parseLambdaBody(params: ParseParams): ParseResult = {
    def loop(remainingBody: Term, listSoFar: List[FocusAction]): ParseResult = {

      remainingBody match {
        case LambdaArgument(idName) if idName == params.argName => Right(listSoFar)
        case LambdaArgument(idName) => FocusError.DidNotDirectlyAccessArgument(idName).asResult

        case OptionSome(Right(remainingCode, action)) => loop(remainingCode, action :: listSoFar)
        case OptionSome(Left(error)) => Left(error)

        case FieldSelect(Right(remainingCode, action)) => loop(remainingCode, action :: listSoFar)
        case FieldSelect(Left(error)) => Left(error)

        case unexpected => FocusError.UnexpectedCodeStructure(unexpected.show).asResult
      }
    }
    loop(params.lambdaBody, Nil)
  }

  private def unwrap(term: Term): Term = {
    term match {
      case Block(List(), inner) => unwrap(inner)
      case Inlined(_, _, inner) => unwrap(inner)
      case x => x
    }
  }

  private object ExpectedLambdaFunction {
    def unapply(term: Term): Option[ParseParams] = 
      unwrap(term) match {
        case Lambda(List(ValDef(argName, typeTree, _)), body) => Some(ParseParams(argName, typeTree.tpe, body))
        case _ => None
      }
  }
}