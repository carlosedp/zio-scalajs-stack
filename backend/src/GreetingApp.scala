package com.carlosedp
package zioscalajs.backend

import zhttp.http._

/** An http app that:
  *   - Accepts a `Request` and returns a `Response`
  *   - Does not fail
  *   - Does not use the environment
  */
object GreetingApp {
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collect[Request] {
      // GET /greet
      case Method.GET -> !! / "" =>
        buildResponse("Index")

      // GET /greet?name=:name
      case req @ (Method.GET -> !! / "greet") if req.url.queryParams.nonEmpty =>
        buildResponse(s"Hello ${req.url.queryParams("name").mkString(" and ")}!")

      // GET /greet
      case Method.GET -> !! / "greet" =>
        buildResponse("Hello World!")

      // GET /greet/:name
      case Method.GET -> !! / "greet" / name =>
        buildResponse(s"Hello $name!")
    }

  def buildResponse(data: String) =
    Response(
      status = Status.Ok,
      headers = Headers.accessControlAllowOrigin("*"),
      data = HttpData.fromString(data),
    )
}
