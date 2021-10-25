package EShop.lab2

//import akka.actor.{ActorSystem, Props, typed}
//import akka.actor.typed.{ActorSystem}

import scala.io.StdIn.readLine
import CartActor.{StartCheckout => CartActorStartCheckout, _}
import Checkout.{StartCheckout => CheckoutStartCheckout, _}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.Breaks.{break, breakable}

object Main extends App {
  import EShop.lab3.OrderManager

//  val system = ActorSystem(new OrderManager().start, "orderManager")
//  system ! OrderManager.Initialize
//  system ! OrderManager.AddItem("lol", "system.systemActorOf()")
//  Await.result(system.whenTerminated, Duration.Inf)
}