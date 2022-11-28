package com.carlosedp
package zioscalajs.backend

import com.carlosedp.zioscalajs.shared.SharedConfig
import zio.*
import zio.http.*
import zio.http.model.Method
import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.middleware.Cors.CorsConfig

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

  // Server config
  val config =
    ServerConfig.default
      .port(SharedConfig.serverPort)
      .leakDetection(LeakDetectionLevel.PARANOID)
      .maxThreads(2)

  val server = Server.serve(httpProg).provide(ServerConfig.live(config), Server.live)

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
