package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*
import zio.test.*

object GreetingAppSpec extends ZIOSpecDefault:

  val greetApp = GreetingApp()
  def spec =
    suite("Greet backend application")(
      test("should greet world") {
        for
          response <- greetApp.runZIO(Request.get(URL(Root / "greet")))
          body     <- response.body.asString
        yield assertTrue(
          response.status == Status.Ok,
          body == "Hello World!",
        )
      },
      test("should greet User if using path") {
        for
          response <- greetApp.runZIO(Request.get(URL(Root / "greet" / "User")))
          body     <- response.body.asString
        yield assertTrue(
          response.status == Status.Ok,
          body == "Hello User!",
        )
      },
      test("should greet User if using query param") {
        for
          response <- greetApp.runZIO(Request.get(URL(Root / "greet", queryParams = QueryParams("name" -> "User"))))
          body     <- response.body.asString
        yield assertTrue(
          response.status == Status.Ok,
          body == "Hello User!",
        )
      },
      test("should greet Users if using multiple query params") {
        for
          response <-
            greetApp.runZIO(Request.get(URL(
              Root / "greet",
              queryParams = QueryParams("name" -> Chunk("User", "User2")),
            )))
          body <- response.body.asString
        yield assertTrue(
          response.status == Status.Ok,
          body == "Hello User and User2!",
        )
      },
    )
