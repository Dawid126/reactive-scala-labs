package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import scala.language.postfixOps

import scala.concurrent.duration._

object TypedCheckout {

  sealed trait Data
  case object Uninitialized                               extends Data
  case class SelectingDeliveryStarted(timer: Cancellable) extends Data
  case class ProcessingPaymentStarted(timer: Cancellable) extends Data

  sealed trait Command
  case object StartCheckout                       extends Command
  case class SelectDeliveryMethod(method: String) extends Command
  case object CancelCheckout                      extends Command
  case object ExpireCheckout                      extends Command
  case class SelectPayment(payment: String)       extends Command
  case object ExpirePayment                       extends Command
  case object ConfirmPaymentReceived              extends Command

  sealed trait Event
  case object CheckOutClosed                        extends Event
  case class PaymentStarted(payment: ActorRef[Any]) extends Event
}

class TypedCheckout {
  import TypedCheckout._

  val checkoutTimerDuration: FiniteDuration = 5 seconds
  val paymentTimerDuration: FiniteDuration  = 5 seconds

  var deliveryMethod = ""
  var paymentMethod = ""

  private def checkoutTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(checkoutTimerDuration, context.self, ExpireCheckout)

  private def paymentTimer(context: ActorContext[Command]): Cancellable =
    context.scheduleOnce(paymentTimerDuration, context.self, ExpirePayment)

  def start: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case StartCheckout =>
        selectingDelivery(checkoutTimer(context))

      case _ =>
        Behaviors.same
    }
  )

  def selectingDelivery(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectDeliveryMethod(method: String) =>
        this.deliveryMethod = method
        timer.cancel()
        selectingPaymentMethod(checkoutTimer(context))

      case ExpireCheckout =>
        timer.cancel()
        println("Checkout time has expired.")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled

      case _ =>
        Behaviors.same
    }
  )

  def selectingPaymentMethod(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case SelectPayment(method: String) =>
        this.paymentMethod = method
        timer.cancel()
        processingPayment(paymentTimer(context))

      case ExpireCheckout =>
        timer.cancel()
        println("Checkout time has expired.")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        cancelled

      case _ =>
        Behaviors.same
    }
  )

  def processingPayment(timer: Cancellable): Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, msg) => msg match {
      case ConfirmPaymentReceived =>
        timer.cancel()
        println("Transaction complete.")
        closed

      case ExpirePayment =>
        timer.cancel()
        println("Payment time has expired.")
        cancelled

      case CancelCheckout =>
        timer.cancel()
        println("Checkout process cancelled.")
        cancelled

      case _ =>
        Behaviors.same
    }
  )

  def cancelled: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      Behaviors.stopped
    }
  )

  def closed: Behavior[TypedCheckout.Command] = Behaviors.receive(
    (_, _) => {
      Behaviors.stopped
    }
  )

}
