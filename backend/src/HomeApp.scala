package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*

object HomeApp:
    def apply(): HttpApp[Any] = Routes(
        Method.GET / "" -> handler(ZIO.succeed(Response.redirect(URL(Root / "greet")))
            @@ MetricsApp.httpHitsMetric("GET", "/"))
    ).toHttpApp
end HomeApp
