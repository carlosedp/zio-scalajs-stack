package com.carlosedp
package zioscalajs.frontend

import com.carlosedp.zioscalajs.shared.SharedConfig
import org.scalajs.dom.*
import sttp.client3.quick.*

object FrontEndApp:

    def main(args: Array[String]): Unit =
        document.addEventListener("DOMContentLoaded", (e: Event) => setupUI())

    def setupUI() =
        val appnode  = document.getElementById("app")
        val _        = addNode(appnode, "Welcome to Scala.js and Vite!!!", "h2")
        val hellodiv = addNode(appnode, "", "div", "hello-div")
        val params   = document.location.search
        val name     = Option(URLSearchParams(params).get("name")).getOrElse("Stranger")
        val _ = queryBackend(s"http://localhost:${SharedConfig.serverPort}/greet?name=${name}", addNode, hellodiv, "h1")
        val _ = addButton(appnode, "Click me", addClickedMessage)
        val _ = addNode(appnode, "You clicked the button " + count.toString + " times.", "p", "clicked-message")

    def addNode(
        targetNode: Node,
        text:       String,
        nodeType:   String = "p",
        id:         String = "",
      ): Node =
        val n = document.createElement(nodeType)
        n.textContent = text
        n.id = id
        targetNode.appendChild(n)

    def updateNode(id: String, text: String) =
        val node = document.getElementById(id)
        node.textContent = text

    def addButton(
        targetNode: Node,
        text:       String,
        onClick:    () => Unit,
      ) =
        val buttonNode = document.createElement("button")
        buttonNode.textContent = text
        buttonNode.addEventListener(
            "click",
            (e: MouseEvent) => onClick(),
        )
        targetNode.appendChild(buttonNode)

    var count = 0
    // @JSExportTopLevel("addClickedMessage")
    def addClickedMessage() =
        count += 1
        updateNode("clicked-message", "You clicked the button " + count.toString + " times.")

    def queryBackend(
        uri:      String,
        callback: (Node, String, String, String) => Node,
        node:     Node,
        nodeType: String,
      ) =
        println(s"Querying backend: $uri...")
        implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
        simpleHttpClient
            .send(quickRequest.get(uri"$uri"))
            .map(response => callback(node, response.body, nodeType, ""))
end FrontEndApp
