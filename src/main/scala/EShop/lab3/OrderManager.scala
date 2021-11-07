package EShop.lab3

import EShop.lab2.{TypedCartActor, TypedCheckout}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object OrderManager {

  sealed trait Command

  case class AddItem(id: String, sender: ActorRef[Ack]) extends Command

  case class RemoveItem(id: String, sender: ActorRef[Ack]) extends Command

  case class SelectDeliveryAndPaymentMethod(delivery: String, payment: String, sender: ActorRef[Ack]) extends Command

  case class Buy(sender: ActorRef[Ack]) extends Command

  case class Pay(sender: ActorRef[Ack]) extends Command

  case class ConfirmCheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Command

  case class ConfirmPaymentStarted(paymentRef: ActorRef[Payment.Command]) extends Command

  case class Initialize(sender: ActorRef[Ack]) extends Command

  case object OperationFailed extends Command

  case object ConfirmPaymentReceived extends Command

  case class WrappedCheckoutResponse(response: TypedCheckout.Event) extends Command

  sealed trait Ack

  case object Done extends Ack //trivial ACK
}

class OrderManager() {

  import OrderManager._

  def start: Behavior[OrderManager.Command] = Behaviors.setup[OrderManager.Command] { context =>
    val checkoutResponseMapper: ActorRef[TypedCheckout.Event] =
      context.messageAdapter(rsp => WrappedCheckoutResponse(rsp))

    def uninitialized: Behavior[OrderManager.Command] = Behaviors.receiveMessage {
      case Initialize(sender) =>
        val cartActor = context.spawn(new TypedCartActor().start, "cart")
        sender ! Done
        open(cartActor)
      case _ =>
        Behaviors.same
    }

    def open(cartActor: ActorRef[TypedCartActor.Command]): Behavior[OrderManager.Command] = Behaviors.receiveMessage {
      case AddItem(id, sender) =>
        cartActor ! TypedCartActor.AddItem(id)
        sender ! Done
        Behaviors.same
      case RemoveItem(id, sender) =>
        cartActor ! TypedCartActor.RemoveItem(id)
        sender ! Done
        Behaviors.same
      case Buy(sender) =>
        cartActor ! TypedCartActor.StartCheckout(context.self)
        inCheckout(cartActor, sender)
      case _ =>
        Behaviors.same

    }

    def inCheckout(
      cartActorRef: ActorRef[TypedCartActor.Command],
      senderRef: ActorRef[Ack]
    ): Behavior[OrderManager.Command] = Behaviors.receiveMessage {
      case ConfirmCheckoutStarted(checkoutRef) =>
        senderRef ! Done
        inCheckout2(checkoutRef)

      case OperationFailed =>
        open(cartActorRef)
      case _ =>
        Behaviors.same

    }

    def inCheckout2(checkoutActorRef: ActorRef[TypedCheckout.Command]): Behavior[OrderManager.Command] = Behaviors.receiveMessage {
      case SelectDeliveryAndPaymentMethod(delivery, payment, sender) =>
        checkoutActorRef ! TypedCheckout.SelectDeliveryMethod(delivery)
        checkoutActorRef ! TypedCheckout.SelectPayment(payment, context.self)
        inPayment(sender)
      case _ =>
        Behaviors.same

    }


    def inPayment(senderRef: ActorRef[Ack]): Behavior[OrderManager.Command] = Behaviors.receiveMessage {
      case ConfirmPaymentStarted(paymentRef) =>
        senderRef ! Done
        inPayment2(paymentRef, senderRef)
      case _ =>
        Behaviors.same
    }

    def inPayment2(paymentActorRef: ActorRef[Payment.Command], senderRef: ActorRef[Ack]): Behavior[OrderManager.Command] = Behaviors.receiveMessage {
      case Pay(sender) =>
        paymentActorRef ! Payment.DoPayment
        sender ! Done
        Behaviors.same
      case ConfirmPaymentReceived =>
        senderRef ! Done
        finished
      case _ =>
        Behaviors.same
    }

    def finished: Behavior[OrderManager.Command] = Behaviors.receiveMessage {
      case wrapped: WrappedCheckoutResponse =>
        wrapped.response match {
          case TypedCheckout.CheckoutClosed =>
            print("checkout closed")
            uninitialized
        }
    }

    uninitialized
  }
}
