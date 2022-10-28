package com.carlosedp
package zioscalajs.backend

import com.carlosedp.zioscalajs.shared.SharedConfig
import zhttp.http.*
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio.*

object MainApp extends ZIOAppDefault {
  val corsConfig = CorsConfig(
    // anyMethod = true,
    // anyOrigin = true,
    allowedOrigins = _ == "*",
    allowedMethods = Some(Set(Method.PUT, Method.DELETE, Method.POST, Method.GET)),
  )

  val console = Console.printLine(s"Server started on http://localhost:${SharedConfig.serverPort}")
  // Add route managers and middleware
  val httpProg = HomeApp() ++ GreetingApp() @@ Middleware.cors(MainApp.corsConfig) @@ Middleware.debug

  val server = Server
    .start(
      port = SharedConfig.serverPort,
      http = httpProg,
    )

  def run = console *> server
}

object HomeApp {
  def apply(): Http[Any, Nothing, Request, Response] =
    // Create CORS configuration
    Http.collect[Request] {
      // GET /, redirect to /greet
      case Method.GET -> !! =>
        Response.redirect("/greet")
    }
}
