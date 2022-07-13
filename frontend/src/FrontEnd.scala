package com.carlosedp
package zioscalajs.frontend

// import scala.scalajs.js.annotation.JSExportTopLevel
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._

import sttp.client3._

object FrontEndApp:

  def addNode(targetNode: dom.Node, text: String, nodeType: String = "p", id: String = "") =
    val parNode = document.createElement(nodeType)
    parNode.textContent = text
    parNode.id = id
    targetNode.appendChild(parNode)

  def updateNode(id: String, text: String) =
    val node = document.getElementById(id)
    node.textContent = text

  def addButton(targetNode: dom.Node, text: String, onClick: () => Unit) =
    val buttonNode = document.createElement("button")
    buttonNode.textContent = text
    buttonNode.addEventListener(
      "click",
      (e: dom.MouseEvent) => onClick(),
    )
    targetNode.appendChild(buttonNode)

  var count = 0
  // @JSExportTopLevel("addClickedMessage")
  def addClickedMessage() =
    count += 1
    updateNode("clicked-message", "You clicked the button " + count.toString + " times.")

  def setupUI() =
    queryBackend("http://localhost:8080/greet?name=Carlos", addNode, document.body, "h2")
    addButton(document.body, "Click me", addClickedMessage)
    addNode(document.body, "You clicked the button " + count.toString + " times.", "p", "clicked-message")

  def main(args: Array[String]): Unit =
    document.addEventListener("DOMContentLoaded", (e: dom.Event) => setupUI())

  def queryBackend(
    uri:      String,
    callback: (dom.Node, String, String, String) => dom.Node,
    node:     dom.Node,
    nodeType: String,
  ) =
    println(s"Querying backend: $uri...")
    sttp.client3.quickRequest
      .get(uri"$uri")
      .send(FetchBackend())
      .map { response =>
        callback(node, response.body, nodeType, "")
      }
