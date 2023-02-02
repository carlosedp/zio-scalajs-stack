package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*
import zio.http.model.Method

// Just redirect requests from "/" to "/greet"
object HomeApp {
  def apply(): Http[Any, Nothing, Request, Response] =
    Http
      .collectZIO[Request] { case Method.GET -> !! =>
        ZIO.succeed(Response.redirect("/greet")) // GET /, redirect to /greet )
      }

}
