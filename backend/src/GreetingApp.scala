package com.carlosedp
package zioscalajs.backend

import zio.*
import zio.http.*

object GreetingApp:
    def apply(): HttpApp[Any] = Routes(
        // GET /greet/:name
        Method.GET / "greet" / string("name") ->
            handler: (name: String, _: Request) =>
                ZIO.succeed(Response.text(s"Hello $name!")) @@ MetricsApp.httpHitsMetric("GET", "/greet"),
        // GET /greet?name=:name or GET /greet?name=:name1&name=:name2 or GET /greet with default name
        Method.GET / "greet" ->
            handler: (req: Request) =>
                val names = req.queryParamsOrElse("name", Seq("World")).mkString(" and ")
                ZIO.succeed(Response.text(s"Hello $names!")) @@ MetricsApp.httpHitsMetric("GET", "/greet"),
    ).toHttpApp
