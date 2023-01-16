package com.carlosedp
package zioscalajs.backend

import zio._
import zio.http.*
import zio.http.model.Method
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.{Metric, MetricLabel}

// Create the Prometheus router exposing metrics in "/metrics" and incrementing a counter
object MetricsApp {
  // Sample metrics
  val gauge1 = Metric.gauge("gauge1")
  val gaugeTest: UIO[Double] = Random.nextDoubleBetween(100.0, 200.0) @@ gauge1
  val httpHits = Metric.counter("httpHits")

  // Map calls to "/greet/some_name" to "/greet/:person" for metric aggregation
  def pathLabelMapper: PartialFunction[Request, String] = { case Method.GET -> !! / "greet" / _ =>
    "/greet/:person"
  }

  def apply() =
    Http.collectZIO[Request] { case Method.GET -> !! / "metrics" =>
      ZIO
        .serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
        .tap(_ =>
          Metric
            .counter("httpHits")
            .tagged(MetricLabel("method", "GET"), MetricLabel("path", "/metrics"))
            .increment,
        )
    }
}
