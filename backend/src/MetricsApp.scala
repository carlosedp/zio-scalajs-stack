package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.Middleware.metrics
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.metrics.{Metric, MetricLabel}

// Create the Prometheus router exposing metrics in "/metrics" and incrementing a counter
object MetricsApp:
    // Sample metrics
    def httpHitsMetric(method: String, path: String) =
        Metric
            .counter("httphits")
            .fromConst(1)
            .tagged(MetricLabel("method", method), MetricLabel("path", path))

    // Create a gauge metric to keep the last generated random double
    val randomdouble: Metric.Gauge[Double] = Metric.gauge("randomdouble")
    // Generate a random double and update the gauge
    val gaugeTest: UIO[Double] =
        for
            double <- Random.nextDoubleBetween(1.0, 200.0) @@ randomdouble
            _      <- ZIO.logInfo(s"Generated a new double: $double")
        yield double

    def apply(): HttpApp[PrometheusPublisher] =
        Routes(
            Method.GET / "metrics" -> handler(
                ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
                    @@ MetricsApp.httpHitsMetric("GET", "/metrics")
            )
        ).toHttpApp @@ metrics()
end MetricsApp
