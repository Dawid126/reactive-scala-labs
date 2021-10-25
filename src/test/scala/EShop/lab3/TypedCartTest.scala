package EShop.lab3

import EShop.lab2.{Cart, TypedCartActor}
import EShop.lab3.TypedCartTest.{cartActorWithCartSizeResponseOnStateChange, emptyMsg, inCheckoutMsg, nonEmptyMsg}
import akka.actor.Cancellable
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, BehaviorTestKit, ScalaTestWithActorTestKit, TestInbox}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TypedCartTest
  extends ScalaTestWithActorTestKit
  with AnyFlatSpecLike
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  override def afterAll: Unit =
    testKit.shutdownTestKit()

  import TypedCartActor._

  //use GetItems command which was added to make test easier
  it should "add item properly" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Romeo & Juliet")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)
  }

  it should "add item properly sync" in {
    val orderManagerInbox = TestInbox[OrderManager.Command]()
    val cartInbox = TestInbox[Cart]()

    val testKit = BehaviorTestKit(new TypedCartActor().start)

    testKit.run(GetItems(cartInbox.ref))
    cartInbox.expectMessage(Cart.empty)

    testKit.run(AddItem("item"))
    val cart = Cart.empty.addItem("item")
    testKit.run(GetItems(cartInbox.ref))
    cartInbox.expectMessage(cart)
  }

  it should "be empty after adding and removing the same item" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Romeo & Juliet")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! RemoveItem("Romeo & Juliet")

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)
  }

  it should "be empty after adding and removing the same item sync" in {
    val orderManagerInbox = TestInbox[OrderManager.Command]()
    val cartInbox = TestInbox[Cart]()

    val testKit = BehaviorTestKit(new TypedCartActor().start)

    testKit.run(GetItems(cartInbox.ref))
    cartInbox.expectMessage(Cart.empty)

    testKit.run(AddItem("item"))
    val cart = Cart.empty.addItem("item")
    testKit.run(GetItems(cartInbox.ref))
    cartInbox.expectMessage(cart)

    testKit.run(RemoveItem("item"))
    testKit.run(GetItems(cartInbox.ref))
    cartInbox.expectMessage(Cart.empty)
  }

  it should "start checkout" in {
    val probe = testKit.createTestProbe[Any]()
    val cart  = cartActorWithCartSizeResponseOnStateChange(testKit, probe.ref)
    val orderManagerProbe = testKit.createTestProbe[OrderManager.Command]

    probe.expectMessage(emptyMsg)
    probe.expectMessage(0)

    cart ! AddItem("Romeo & Juliet")

    probe.expectMessage(nonEmptyMsg)
    probe.expectMessage(1)

    cart ! StartCheckout(orderManagerProbe.ref)

    probe.expectMessage(inCheckoutMsg)
    probe.expectMessage(1)
  }

  it should "start checkout sync" in {
    val orderManagerInbox = TestInbox[OrderManager.Command]()
    val testInbox = TestInbox[String]()
    val cartInbox = TestInbox[Cart]()

    val overriddenTypedCartActor = new TypedCartActor() {
      override def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] =
        Behaviors.setup(_ => {
          testInbox.ref ! inCheckoutMsg
          super.inCheckout(cart)
        })
    }

    val testKit = BehaviorTestKit(overriddenTypedCartActor.start)

    testKit.run(GetItems(cartInbox.ref))
    cartInbox.expectMessage(Cart.empty)

    testKit.run(AddItem("item"))
    val cart = Cart.empty.addItem("item")
    testKit.run(GetItems(cartInbox.ref))
    cartInbox.expectMessage(cart)

    testKit.run(StartCheckout(orderManagerInbox.ref))
    testInbox.expectMessage(inCheckoutMsg)
  }
}

object TypedCartTest {
  val emptyMsg      = "empty"
  val nonEmptyMsg   = "nonEmpty"
  val inCheckoutMsg = "inCheckout"

  def cartActorWithCartSizeResponseOnStateChange(
                                                  testKit: ActorTestKit,
                                                  probe: ActorRef[Any]
                                                ): ActorRef[TypedCartActor.Command] =
    testKit.spawn {
      val cartActor = new TypedCartActor {
        override val cartTimerDuration: FiniteDuration = 1.seconds

        override def empty: Behavior[TypedCartActor.Command] =
          Behaviors.setup(_ => {
            probe ! emptyMsg
            probe ! 0
            super.empty
          })

        override def nonEmpty(cart: Cart, timer: Cancellable): Behavior[TypedCartActor.Command] =
          Behaviors.setup(_ => {
            probe ! nonEmptyMsg
            probe ! cart.size
            super.nonEmpty(cart, timer)
          })

        override def inCheckout(cart: Cart): Behavior[TypedCartActor.Command] =
          Behaviors.setup(_ => {
            probe ! inCheckoutMsg
            probe ! cart.size
            super.inCheckout(cart)
          })

      }
      cartActor.start
    }

}


