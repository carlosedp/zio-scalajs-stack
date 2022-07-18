package com.carlosedp
package zioscalajs.backend

import zhttp.http.Middleware.cors
import zhttp.http._
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio._

private val PORT = 8080

// Create CORS configuration
private val corsConfig =
  CorsConfig(
    allowedOrigins = _ == "*",
    allowedMethods = Some(Set(Method.PUT, Method.DELETE, Method.POST, Method.GET)),
  )

object MainApp extends ZIOAppDefault:
  def run     = console *> server
  val console = Console.printLine(s"Server started on http://localhost:${PORT}")

  val server = Server
    .start(
      port = PORT,
      http = HomeApp() ++ GreetingApp(),
    )

object HomeApp {
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collect[Request] {
      // GET /, redirect to /greet
      case Method.GET -> !! =>
        Response.redirect("/greet")
    } @@ cors(corsConfig)
}
