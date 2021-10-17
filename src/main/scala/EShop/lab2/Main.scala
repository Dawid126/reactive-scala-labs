package EShop.lab2

import akka.actor.{ActorSystem, Props, typed}

import scala.io.StdIn.readLine
import CartActor.{StartCheckout => CartActorStartCheckout, _}
import Checkout.{StartCheckout => CheckoutStartCheckout, _}

import scala.util.control.Breaks.{break, breakable}

object Main extends App {
  val system = ActorSystem("EShop")
  val cartActor = system.actorOf(Props[CartActor], "mainActor")
  var checkoutCounter: Int = 0

  start

  def start: Unit = {

    while (true) {
      println("Available operations:\n" +
        "1) AddItem <item_name>\n" +
        "2) RemoveItem <item_name>\n" +
        "3) Checkout\n" +
        "4) Print")

      breakable {
        val invokedOperation = readLine().split(" ")
        if (invokedOperation.size > 2) {
          println("Wrong number of arguments.")
          break
        }

        val firstArgument = invokedOperation(0)

        firstArgument match {
          case "AddItem" =>
            cartActor ! AddItem(invokedOperation(1))
          case "RemoveItem" =>
            cartActor ! RemoveItem(invokedOperation(1))
          case "Checkout" =>
            handleCheckout
          case "Print" =>
            cartActor ! Print
          case _ =>
            println("Wrong option.")
        }
      }
    }
  }

  private def handleCheckout: Unit = {
    cartActor ! CartActorStartCheckout
    val checkoutActor = system.actorOf(Props[Checkout], "checkoutActor" + checkoutCounter)
    checkoutCounter = checkoutCounter + 1
    checkoutActor ! CheckoutStartCheckout
    println("Enter delivery method (or cancel):")
    val deliveryMethod = readLine()

    if (deliveryMethod.toLowerCase() == "cancel") {
      checkoutActor ! CancelCheckout
      cartActor ! ConfirmCheckoutCancelled
    }
    else {
      checkoutActor ! SelectDeliveryMethod(deliveryMethod)
      println("Enter payment method (or cancel):")
      val paymentMethod = readLine()

      if (paymentMethod.toLowerCase() == "cancel") {
        checkoutActor ! CancelCheckout
        cartActor ! ConfirmCheckoutCancelled
      }
      else {
        checkoutActor ! SelectPayment(paymentMethod)
        println("Pay (enter \"pay\") (or cancel):")
        val payment = readLine()

        if (payment.toLowerCase() == "cancel") {
          checkoutActor ! CancelCheckout
          cartActor ! ConfirmCheckoutCancelled
        }
        else {
          checkoutActor ! ConfirmPaymentReceived
          cartActor ! ConfirmCheckoutClosed
        }
      }
    }
  }
}
