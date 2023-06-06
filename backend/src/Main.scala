package com.carlosedp
package zioscalajs.backend

import com.carlosedp.zioscalajs.shared.SharedConfig
import zio.*
import zio.http.Header.{AccessControlAllowMethods, AccessControlAllowOrigin, Origin}
import zio.http.*
import zio.http.internal.middlewares.Cors.CorsConfig
import zio.http.netty.NettyConfig
import zio.http.netty.NettyConfig.LeakDetectionLevel
import zio.logging.*
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus.{prometheusLayer, publisherLayer}

object Main extends ZIOAppDefault {
  // Create CORS configuration
  val corsConfig: CorsConfig =
    CorsConfig(
      allowedOrigin = {
        case origin @ Origin.Value(_, host, _) if host == "dev" => Some(AccessControlAllowOrigin.Specific(origin))
        case _                                                  => None
      },
      allowedMethods = AccessControlAllowMethods(Method.PUT, Method.DELETE, Method.POST, Method.GET),
    )

  // Configure ZIO Logging
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> consoleLogger(
      ConsoleLoggerConfig(LogFormat.colored, LogFilter.acceptAll),
    ) ++ logMetrics

  // Add routes and middleware
  val httpRoutes = (MetricsApp() ++ HomeApp() ++ GreetingApp()) @@
    HttpAppMiddleware.cors(corsConfig) @@
    HttpAppMiddleware.metrics(MetricsApp.pathLabelMapper) @@
    HttpAppMiddleware.timeout(5.seconds) @@
    HttpAppMiddleware.debug

  // ZIO-HTTP server config
  val configLayer =
    ZLayer.succeed(
      Server.Config.default
        .port(SharedConfig.serverPort),
    )

  val nettyConfigLayer = ZLayer.succeed(
    NettyConfig.default
      .leakDetection(LeakDetectionLevel.DISABLED)
      .maxThreads(8),
  )

  // Define ZIO-http server
  val server: ZIO[Any, Throwable, Nothing] = Server
    .serve(httpRoutes)
    .provide( // Add required layers
      configLayer,
      nettyConfigLayer,
      Server.customized,
      publisherLayer,
      prometheusLayer,
      ZLayer.succeed(MetricsConfig(500.millis)), // Metrics pull interval from internal store
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
