package com.carlosedp
package zioscalajs.backend

import com.carlosedp.zioscalajs.shared.SharedConfig
import zio.*
import zio.http.*
import zio.http.middleware.Cors.CorsConfig
import zio.http.model.Method
import zio.logging.{LogFormat, consoleJson, logMetrics}
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}

object Main extends ZIOAppDefault {
  // Set CORS config
  val corsConfig = CorsConfig(
    allowedOrigins = _ == "*",                                                     // anyOrigin = true,
    allowedMethods = Some(Set(Method.PUT, Method.DELETE, Method.POST, Method.GET)),// anyMethod = true,
  )

  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleJson(LogFormat.colored) ++ logMetrics

  // Add routes and middleware
  val httpRoutes = (MetricsApp() ++ HomeApp() ++ GreetingApp()) @@
    Middleware.cors(corsConfig) @@
    Middleware.metrics(MetricsApp.pathLabelMapper) @@
    Middleware.timeout(5.seconds) @@
    Middleware.debug

  // ZIO-http server config
  val config: ServerConfig =
    ServerConfig.default
      .port(SharedConfig.serverPort)
      .leakDetection(ServerConfig.LeakDetectionLevel.PARANOID)
      .maxThreads(2)

  // Define ZIO-http server
  val server: ZIO[Any, Throwable, Nothing] = Server
    .serve(httpRoutes)
    .provide( // Add required layers
      ServerConfig.live(config),
      Server.live,
      publisherLayer,
      prometheusLayer,
      ZLayer.succeed(MetricsConfig(200.millis)), // Metrics pull interval from internal store
    )

  // Run the application
  def run: ZIO[Scope, Any, ExitCode] =
    for {
      _ <- ZIO.logInfo(s"Server started at http://localhost:${SharedConfig.serverPort}")
      _ <- server.forkDaemon
      f <- MetricsApp.gaugeTest.schedule(Schedule.spaced(5.second)).fork
      _ <- ZIO.logInfo(s"Started gaugetest with random Double every second")
      _ <- f.join
    } yield ExitCode.success

}
