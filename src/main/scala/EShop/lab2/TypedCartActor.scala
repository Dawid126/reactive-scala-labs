package EShop.lab2

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import scala.language.postfixOps

import scala.concurrent.duration._

object TypedCartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command

  case object Print                    extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef[TypedCheckout.Command]) extends Event
}

class TypedCartActor {

  import TypedCartActor._

  val cartTimerDuration: FiniteDuration = 5 seconds

  private def scheduleTimer(context: ActorContext[TypedCartActor.Command]): Cancellable =
    context.scheduleOnce(cartTimerDuration, context.self, ExpireCart)

  def start: Behavior[TypedCartActor.Command] = empty

  def empty: Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case AddItem(item) =>
        println("Item added: " + item)
        nonEmpty(Cart(Seq(item)), scheduleTimer(context))

      case Print =>
        println("Current cart: empty")
        Behaviors.same

      case _ =>
        Behaviors.same
    }
  )

  def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case AddItem(item) =>
        timer.cancel()
        println("Item added: " + item)
        nonEmpty(cart.addItem(item), scheduleTimer(context))

      case RemoveItem(item) =>
        if (cart.contains(item)) {
          timer.cancel()
          println("Item removed: " + item)
          if(cart.size > 1) {
            nonEmpty(cart.removeItem(item), scheduleTimer(context))
          }
          else {
            empty
          }
        }
        else {
          println("No such item in the cart.")
          Behaviors.same
        }
      case StartCheckout =>
        timer.cancel()
        inCheckout(cart)

      case ExpireCart =>
        timer.cancel()
        println("Cart tine has expired.")
        empty

      case Print =>
        println("Current cart content: " + cart)
        Behaviors.same

      case _ =>
        Behaviors.same
    }
  )

  def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] = Behaviors.receive(
    (context, msg) => msg match {
      case ConfirmCheckoutCancelled =>
        nonEmpty(cart, scheduleTimer(context))

      case ConfirmCheckoutClosed =>
        println("Cart closed after checkout")
        empty

      case Print =>
        println("Current cart: " + cart)
        Behaviors.same

      case _ =>
        Behaviors.same
    }
  )

}
