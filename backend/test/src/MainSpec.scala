package com.carlosedp
package zioscalajs.backend

import com.carlosedp.zioscalajs.backend.MainApp
import zhttp.http.*
import zio._
import zio.test.Assertion._
import zio.test._

object MainSpec extends ZIOSpecDefault:

  override def spec =
    suite("Main backend application")(
      test("should start") {
        for {
          _      <- MainApp.console
          output <- TestConsole.output
        } yield assertTrue(output.head contains "started")
      },
    )
