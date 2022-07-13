package com.carlosedp
package zioscalajs.backend

import zhttp.http._
import zhttp.service.Server
import zio._

private val PORT = 8080

object MainApp extends ZIOAppDefault:

  def run = console *> server

  val console = Console.printLine(s"Server started on http://localhost:${PORT}")

  val server = Server
    .start(
      port = PORT,
      http = GreetingApp(),
    )
