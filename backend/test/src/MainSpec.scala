package com.carlosedp
package zioscalajs.backend

import zhttp.http.*
import zio.test.*

object MainSpec extends ZIOSpecDefault {

  def homeApp = HomeApp()
  def spec =
    suite("Main backend application")(
      test("should show start message") {
        for {
          _      <- MainApp.console
          output <- TestConsole.output
        } yield assertTrue(output.head contains "started")
      },
      test("root route should redirect to /greet") {
        for {
          response <- homeApp(Request(url = URL(!!)))
          body     <- response.body.asString
        } yield assertTrue(
          response.status == Status.TemporaryRedirect,
          response.headers == Headers.location("/greet"),
          body.isEmpty,
        )
      },
    )
}
