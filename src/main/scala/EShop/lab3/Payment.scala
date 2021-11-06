package EShop.lab3

import EShop.lab2.TypedCheckout
import EShop.lab2.TypedCheckout.ConfirmPaymentReceived
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Payment {

  sealed trait Command
  case object DoPayment extends Command

  sealed trait Event
  case object PaymentConfirmed extends Event
}

class Payment(
  method: String,
  orderManager: ActorRef[OrderManager.Command],
  checkout: ActorRef[TypedCheckout.Command]
) {

  import Payment._

  def start: Behavior[Payment.Command] = Behaviors.receive {
    (context, msg) => msg match {
      case DoPayment =>
        checkout ! TypedCheckout.ConfirmPaymentReceived
        orderManager ! OrderManager.ConfirmPaymentReceived
        closed
      case _ =>
        Behaviors.same
    }
  }

  def closed: Behavior[Payment.Command] = Behaviors.receive(
    (_, msg) => msg match {
      case _ =>
        Behaviors.stopped
    }
  )

}
