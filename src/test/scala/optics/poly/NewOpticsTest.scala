package optics.poly

import functions.Index

class NewOpticsTest extends munit.FunSuite {

  case class Office(desk: Desk)
  case class Printer(pcLoadLetter: Boolean)
  case class Desk(numPens: Int, printer: Option[Printer])


  test("GetOne andThen GetOne") {
    val deskOptic: Optic[GetOne, Office, Office, Desk, Desk] = 
      Optic.withGetOne(_.desk)

    val pensOptic: Optic[GetOne, Desk, Desk, Int, Int] = 
      Optic.withGetOne(_.numPens)

    val composed: Optic[GetOne, Office, Office, Int, Int] = 
      deskOptic.andThen(pensOptic)

    val office = Office(Desk(5, None))

    assertEquals(composed.get(office), 5)
  }

  test("GetOption andThen GetOne") {
    val printerOptic: Optic[GetOption, Desk, Desk, Printer, Printer] = 
      Optic.withGetOption[Desk, Printer](_.printer)

    val pcLoadLetterOptic: Optic[GetOne, Printer, Printer, Boolean, Boolean] = 
      Optic.withGetOne[Printer, Boolean](_.pcLoadLetter)

    val composed: Optic[GetOption, Desk, Desk, Boolean, Boolean] = 
      printerOptic.andThen(pcLoadLetterOptic)

    val desk = Desk(5, Some(Printer(true)))
    assertEquals(composed.getOption(desk), Some(true))
  }

}
