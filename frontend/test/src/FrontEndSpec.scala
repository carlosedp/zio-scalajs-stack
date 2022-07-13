package com.carlosedp
package zioscalajs.frontend

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.ext._
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class FrontEndSpec extends AnyFlatSpec with should.Matchers {
  // Initialize App
  FrontEndApp.setupUI()

  behavior of "Frontend App"

  // it should "contain 'Hello world!' text in its body" in {
  //   document.querySelectorAll("p").count(_.textContent.contains("Hello world!")) should be(1)
  // }

  it should "contain a button in its body" in {
    document.querySelectorAll("button").count(_.textContent.contains("Click me")) should be(1)
  }

  it should "append 'You clicked the button!' text when the user clicks on the 'Click me' button" in {
    def messageCount = document.getElementById("clicked-message").textContent
    val button       = document.querySelector("button").asInstanceOf[dom.html.Button]

    button should not be null
    messageCount should include("You clicked the button 0 times")

    for (c <- 1 to 5) {
      button.click()
      messageCount should include(s"You clicked the button ${c} times")
    }
  }
}
