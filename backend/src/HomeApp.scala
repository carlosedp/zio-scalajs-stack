package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*

// Just redirect requests from "/" to "/greet"
object HomeApp:
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collectZIO[Request] { case Method.GET -> Root =>
      ZIO.succeed(Response.redirect(URL(Root / "greet"))) // GET /, redirect to /greet )
    }
