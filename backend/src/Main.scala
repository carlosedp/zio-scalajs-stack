package com.carlosedp
package zioplay.backend

import zio.Console._
import zio._

object MyApp extends ZIOAppDefault:
  def run = myAppLogic.exitCode

  def hello = "Hello"

  val myAppLogic =
    for
      _    <- Console.print("Please enter your name: ")
      name <- Console.readLine
      _    <- Console.printLine(hello ++ s", $name!")
    yield ()
