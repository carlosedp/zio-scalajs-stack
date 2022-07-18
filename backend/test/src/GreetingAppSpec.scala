package com.carlosedp
package zioscalajs.backend

import zhttp.http._
import zio._
import zio.test.Assertion._
import zio.test._

object GreetingAppSpec extends ZIOSpecDefault:

  val greetApp = GreetingApp()
  def spec =
    suite("Greet backend application")(
      test("should greet world") {
        for {
          response <- greetApp(Request(url = URL(!! / "greet")))
          body     <- response.bodyAsString
        } yield assertTrue(
          response.status == Status.Ok,
          body == "Hello World!",
        )
      },
      test("should greet User if using path") {
        for {
          response <- greetApp(Request(url = URL(!! / "greet" / "User")))
          body     <- response.bodyAsString
        } yield assertTrue(
          response.status == Status.Ok,
          body == "Hello User!",
        )
      },
      test("should greet User if using param") {
        for {
          response <- greetApp(Request(url = URL(!! / "greet").setQueryParams("?name=User")))
          body     <- response.bodyAsString
        } yield assertTrue(
          response.status == Status.Ok,
          body == "Hello User!",
        )
      },
    )
