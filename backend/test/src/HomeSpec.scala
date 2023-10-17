package com.carlosedp
package zioscalajs.backend

import zio.http.*
import zio.test.*

object HomeSpec extends ZIOSpecDefault:

  def spec =
    suite("Main backend application")(
      test("root route should redirect to /greet"):
        for
          response <- HomeApp().runZIO(Request.get(URL(Root)))
          body     <- response.body.asString
        yield assertTrue(
          response.status == Status.TemporaryRedirect,
          response.headers(Header.Location).contains(Header.Location(URL(Root / "greet"))),
          body.isEmpty,
        )
    )
