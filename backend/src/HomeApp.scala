package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*

object HomeApp:
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request]:
      // GET /, redirect to /greet )
      case Method.GET -> Root =>
        ZIO.succeed(Response.redirect(URL(Root / "greet")))
