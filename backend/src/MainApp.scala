package com.carlosedp
package zioscalajs.backend

import com.carlosedp.zioscalajs.shared.SharedConfig
import zhttp.http.Middleware.cors
import zhttp.http.*
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio.*

object MainApp extends ZIOAppDefault {
  val corsConfig = CorsConfig(
    allowedOrigins = _ == "*",
    allowedMethods = Some(Set(Method.PUT, Method.DELETE, Method.POST, Method.GET)),
  )
  def run     = console *> server
  val console = Console.printLine(s"Server started on http://localhost:${SharedConfig.serverPort}")

  val server = Server
    .start(
      port = SharedConfig.serverPort,
      http = HomeApp() ++ GreetingApp(),
    )

  object HomeApp {
    def apply(): Http[Any, Nothing, Request, Response] =
      // Create CORS configuration
      Http.collect[Request] {
        // GET /, redirect to /greet
        case Method.GET -> !! =>
          Response.redirect("/greet")
      } @@ cors(MainApp.corsConfig)
  }
}
