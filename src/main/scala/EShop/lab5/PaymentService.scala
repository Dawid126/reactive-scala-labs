package EShop.lab5

import EShop.lab5.Payment.{Message, WrappedPaymentServiceResponse}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object PaymentService {

  sealed trait Response
  case object PaymentSucceeded extends Response

  case class PaymentClientError() extends Exception
  case class PaymentServerError() extends Exception



  // actor behavior which needs to be supervised
  // use akka.http.scaladsl.Http to make http based payment request
  // use getUri method to obtain url
  def apply(
    method: String,
    payment: ActorRef[Message]
  ): Behavior[HttpResponse] = Behaviors.setup { context =>
    val http = Http(context.system)
//    val result = http
//      .singleRequest(HttpRequest(getURI(method)))
//    context.pipeToSelf(result)
    val res = http.singleRequest(HttpRequest(uri=getURI(method)))
    context.pipeToSelf(res) {
      case Success(value) => value
      case Failure(e)     => throw e
    }

    def shutdown(context: ActorContext[_]) = {
      Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
      context.system.terminate()
    }
    // for Actor Materializer
    implicit val system = context.system

    Behaviors.receiveMessage {
      case resp @ HttpResponse(code, _, _, _) => code.intValue() match {
        case 200 => payment ! WrappedPaymentServiceResponse(PaymentSucceeded)
          Behaviors.stopped
        case 400 | 404 => throw new PaymentClientError
        case 408 | 418 | 500 => throw new PaymentServerError
      }
    }
  }

  // remember running PymentServiceServer() before trying payu based payments
  private def getURI(method: String) = method match {
    case "payu"   => "http://127.0.0.1:12000"
    case "paypal" => s"http://httpbin.org/status/408"
    case "visa"   => s"http://httpbin.org/status/200"
    case _        => s"http://httpbin.org/status/404"
  }
}
