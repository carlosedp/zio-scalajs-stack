package com.carlosedp
package zioscalajs.backend

import zio._
import zio.test.Assertion._
import zio.test._
import zhttp.http.*
import com.carlosedp.zioscalajs.backend.MainApp

object MainSpec extends ZIOSpecDefault:

  override def spec =
    suite("Main backend application")(
      test("should start") {
        for {
          _      <- MainApp.main
          output <- TestConsole.output
        } yield assertTrue(output.head contains "started")
      },
    )
