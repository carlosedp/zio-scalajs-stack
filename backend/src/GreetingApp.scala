package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*

/**
 * An http app that:
 *   - Accepts a `Request` and returns a `Response`
 *   - Does not fail
 *   - Does not use the environment
 */
object GreetingApp:
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request]:
      // GET /greet?name=:name or GET /greet?name=:name1&name=:name2
      case req @ (Method.GET -> Root / "greet") if req.url.queryParams.nonEmpty =>
        val names = req.url.queryParams.get("name").get.mkString(" and ")
        ZIO.succeed(Response.text(s"Hello $names!")) @@ MetricsApp.httpHitsMetric("GET", s"/greet/$names")

      // GET /greet/:name
      case Method.GET -> Root / "greet" / name =>
        ZIO.succeed(Response.text(s"Hello $name!")) @@ MetricsApp.httpHitsMetric("GET", s"/greet/$name")

      // GET /greet
      case Method.GET -> Root / "greet" =>
        // httpHitsMetric("GET", "/greet").increment.
        ZIO.succeed(Response.text("Hello World!")) @@ MetricsApp.httpHitsMetric("GET", "/greet")
