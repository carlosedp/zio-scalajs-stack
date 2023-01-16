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
  val httpRoutes = (MetricsApp() ++ HomeApp() ++ GreetingApp()) @@ Middleware.cors(
    corsConfig,
  ) @@ Middleware.metrics(MetricsApp.pathLabelMapper) @@ Middleware.debug

  // ZIO-http server config
  val config =
    ServerConfig.default
      .port(SharedConfig.serverPort)
      .leakDetection(ServerConfig.LeakDetectionLevel.PARANOID)
      .maxThreads(2)

  // Define ZIO-http server
  val server = Server
    .serve(httpRoutes)
    .provide( // Add required layers
      ServerConfig.live(config),
      Server.live,
      publisherLayer,
      prometheusLayer,
      ZLayer.succeed(MetricsConfig(200.millis)), // Metrics pull interval from internal store
    )

  def run: ZIO[Scope, Any, ExitCode] =
    for {
      _ <- ZIO.logInfo(s"Server started at http://localhost:${SharedConfig.serverPort}")
      _ <- server.forkDaemon
      _ <- ZIO.logInfo(s"Started gaugetest with random Double every second")
      f <- MetricsApp.gaugeTest.schedule(Schedule.spaced(1.second)).fork
      _ <- f.join
    } yield ExitCode.success

}
