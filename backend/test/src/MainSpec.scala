package com.carlosedp
package zioplay.backend

import zio._
import zio.test.Assertion._
import zio.test._

object MainSpec extends ZIOSpecDefault:

  def spec = suite("MainSpec")(
    test("hello function returns hello") {
      assertTrue(MyApp.hello == "Hello")
    },
    test("Main correctly displays output") {
      for {
        _      <- TestConsole.feedLines("User")
        _      <- ZIO.collectAll(List.fill(1)(MyApp.myAppLogic))
        output <- TestConsole.output
      } yield assertTrue(output == Vector("Please enter your name: ", "Hello, User!\n"))
    },
  )
