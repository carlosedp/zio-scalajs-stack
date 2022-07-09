package com.carlosedp
package zioscalajs.frontend

// import scala.scalajs.js.annotation.JSExportTopLevel

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html._

object FrontEndApp:

  def appendPar(targetNode: dom.Node, text: String) =
    val parNode = document.createElement("p")
    parNode.textContent = text
    targetNode.appendChild(parNode)

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
    appendPar(document.body, "You clicked the button " + count.toString + " times.")
    count += 1

  def setupUI() =
    appendPar(document.body, "Hello world!")
    addButton(document.body, "Click me", addClickedMessage)

  def main(args: Array[String]): Unit =
    document.addEventListener("DOMContentLoaded", (e: dom.Event) => setupUI())
