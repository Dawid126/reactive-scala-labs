package EShop.lab3

import EShop.lab2.{Cart, TypedCartActor, TypedCheckout}
import EShop.lab3.OrderManager.ConfirmPaymentStarted
import akka.actor.Cancellable
import akka.actor.testkit.typed.Effect.{Scheduled, Spawned}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration._
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, ScalaTestWithActorTestKit, TestInbox}
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps

class TypedCheckoutTest extends ScalaTestWithActorTestKit
  with AnyFlatSpecLike
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures {

  import TypedCheckout._

  override def afterAll: Unit =
    testKit.shutdownTestKit()

  it should "Send close confirmation to cart async" in {
    val probe             = testKit.createTestProbe[String]
    val orderManagerProbe = testKit.createTestProbe[OrderManager.Command]
    val cartActor = testKit.createTestProbe[TypedCartActor.Command]
    val checkoutActor = testKit.spawn {
      val checkout = new TypedCheckout(cartActor.ref)
      checkout.start
    }

    checkoutActor ! TypedCheckout.StartCheckout
    checkoutActor ! SelectDeliveryMethod("delivery")
    checkoutActor ! SelectPayment("payment", orderManagerProbe.ref)
    checkoutActor ! ConfirmPaymentReceived
    cartActor.expectMessage(TypedCartActor.ConfirmCheckoutClosed)
  }

  it should "Send close confirmation to cart sync" in {
    val orderManagerInbox = TestInbox[OrderManager.Command]()
    val cartActorInbox = TestInbox[TypedCartActor.Command]()

    val testKit = BehaviorTestKit(new TypedCheckout(cartActorInbox.ref).start)
    testKit.run(TypedCheckout.StartCheckout)
    testKit.run(TypedCheckout.SelectDeliveryMethod("delivery"))
    testKit.run(TypedCheckout.SelectPayment("payment", orderManagerInbox.ref))
    testKit.run(TypedCheckout.ConfirmPaymentReceived)
    cartActorInbox.expectMessage(TypedCartActor.ConfirmCheckoutClosed)
  }
}
