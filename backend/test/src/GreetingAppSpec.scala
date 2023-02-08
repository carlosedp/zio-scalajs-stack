package com.carlosedp
package zioscalajs.backend

import zio.http.*
import zio.http.model.*
import zio.test.*

object GreetingAppSpec extends ZIOSpecDefault {

  val greetApp = GreetingApp()
  def spec =
    suite("Greet backend application")(
      test("should greet world") {
        for {
          response <- greetApp.runZIO(Request.get(URL(!! / "greet")))
          body     <- response.body.asString
        } yield assertTrue(
          response.status == Status.Ok,
          body == "Hello World!",
        )
      },
      test("should greet User if using path") {
        for {
          response <- greetApp.runZIO(Request.get(URL(!! / "greet" / "User")))
          body     <- response.body.asString
        } yield assertTrue(
          response.status == Status.Ok,
          body == "Hello User!",
        )
      },
      test("should greet User if using param") {
        for {
          response <- greetApp.runZIO(Request.get(URL(!! / "greet").setQueryParams("?name=User")))
          body     <- response.body.asString
        } yield assertTrue(
          response.status == Status.Ok,
          body == "Hello User!",
        )
      },
    )
}
