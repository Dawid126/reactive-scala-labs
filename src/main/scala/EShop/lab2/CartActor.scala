package EShop.lab2

import akka.actor.{Actor, ActorRef, Cancellable, Props, Timers}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import scala.language.postfixOps

object CartActor {

  sealed trait Command
  case class AddItem(item: Any)        extends Command
  case class RemoveItem(item: Any)     extends Command
  case object ExpireCart               extends Command
  case object StartCheckout            extends Command
  case object ConfirmCheckoutCancelled extends Command
  case object ConfirmCheckoutClosed    extends Command
  case object Print                    extends Command

  sealed trait Event
  case class CheckoutStarted(checkoutRef: ActorRef) extends Event

  def props = Props(new CartActor())
}

class CartActor extends Actor with Timers {
  import context._
  import CartActor._

  private val log       = Logging(context.system, this)
  val cartTimerDuration = 10 seconds

  private def scheduleTimer: Cancellable =
    system.scheduler.scheduleOnce(cartTimerDuration, self, ExpireCart)

  def receive: Receive = empty

  def empty: Receive =  {
    case AddItem(item) =>
      println("Item added: " + item)
      context become nonEmpty(Cart.empty.addItem(item), scheduleTimer)

    case Print =>
      println("Current cart is empty.")

    case _ =>
  }

  def nonEmpty(cart: Cart, timer: Cancellable): Receive = {
    case AddItem(item) =>
      timer.cancel()
      context become nonEmpty(cart.addItem(item), scheduleTimer)

    case RemoveItem(item) =>
      if (cart.contains(item)) {
        timer.cancel()
        println("Item removed: " + item)
        if(cart.size > 1) {
          context become nonEmpty(cart.removeItem(item), scheduleTimer)
        }
        else {
          context become empty
        }
      }
      else {
        println("No such item in the cart.")
      }

    case StartCheckout =>
      timer.cancel()
      context become inCheckout(cart)

    case ExpireCart =>
      timer.cancel()
      println("Cart time has expired.")
      context become empty

    case Print =>
      println("Current cart content: " + cart)

    case _ =>
  }

  def inCheckout(cart: Cart): Receive = {
    case ConfirmCheckoutCancelled =>
      println("Checkout canceled.")
      context become nonEmpty(cart, scheduleTimer)

    case ConfirmCheckoutClosed =>
      println("Cart closed after checkout.")
      context become empty

    case Print =>
      println("Current cart content in checkout: " + cart)

    case _ =>
  }
}
