package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.{Metric, MetricLabel}

def httpHitsMetric(method: String, path: String) =
  Metric
    .counter("httphits")
    .fromConst(1)
    .tagged(MetricLabel("method", method), MetricLabel("path", path))

// Create the Prometheus router exposing metrics in "/metrics" and incrementing a counter
object MetricsApp {
  // Sample metrics
  val randomdouble = Metric.gauge("randomdouble")
  val gaugeTest: UIO[Double] =
    for {
      double <- Random.nextDoubleBetween(1.0, 200.0) @@ randomdouble
      _      <- ZIO.logInfo(s"Generated a new double: $double")
    } yield double

  // Map calls to "/greet/some_name" to "/greet/:person" for metric aggregation
  def pathLabelMapper: PartialFunction[Request, String] = { case Method.GET -> !! / "greet" / _ =>
    "/greet/:person"
  }

  def apply(): Http[PrometheusPublisher, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.GET -> !! / "metrics" =>
      ZIO.serviceWithZIO[PrometheusPublisher](
        _.get.map(Response.text),
      ) @@ httpHitsMetric("GET", "/metrics")
    }
}
