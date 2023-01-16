package com.carlosedp
package zioscalajs.backend

import zio.http.*
import zio.http.model.*
import zio.test.*

object HomeSpec extends ZIOSpecDefault {

  def spec =
    suite("Main backend application")(
      test("root route should redirect to /greet") {
        for {
          response <- HomeApp()(Request.get(URL(!!)))
          body     <- response.body.asString
        } yield assertTrue(
          response.status == Status.TemporaryRedirect,
          response.headers == Headers.location("/greet"),
          body.isEmpty,
        )
      },
    )
}
