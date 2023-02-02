package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*
import zio.http.model.Method

/** An http app that:
  *   - Accepts a `Request` and returns a `Response`
  *   - Does not fail
  *   - Does not use the environment
  */
object GreetingApp {
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] {
      // GET /greet?name=:name
      case req @ (Method.GET -> !! / "greet") if req.url.queryParams.nonEmpty =>
        ZIO.succeed(Response.text(s"Hello ${req.url.queryParams("name").mkString(" and ")}!"))
        @@ httpHitsMetric ("GET", "/greet/$names")

      // GET /greet/:name
      case Method.GET -> !! / "greet" / name =>
        ZIO.succeed(Response.text(s"Hello $name!")) @@ httpHitsMetric("GET", s"/greet/$name")

      // GET /greet
      case Method.GET -> !! / "greet" =>
        // httpHitsMetric("GET", "/greet").increment.
        ZIO.succeed(Response.text("Hello World!")) @@ httpHitsMetric("GET", "/greet")
    }
}
