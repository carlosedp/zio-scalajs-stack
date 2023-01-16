package com.carlosedp
package zioscalajs.backend

import zio.http.*
import zio.http.model.Method

// Just redirect requests from "/" to "/greet"
object HomeApp {
  def apply(): Http[Any, Nothing, Request, Response] =
    // Create CORS configuration
    Http.collect[Request] {
      // GET /, redirect to /greet
      case Method.GET -> !! =>
        Response
          .redirect("/greet")
    }
}
