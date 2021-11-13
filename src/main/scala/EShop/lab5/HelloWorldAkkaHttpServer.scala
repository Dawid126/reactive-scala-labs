package EShop.lab5

import EShop.lab5.HelloWorldAkkaHttpServer.Response
import EShop.lab5.ProductCatalog.{GetItems, Item}
import EShop.lab5.ProductCatalog.Items
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.remote.transport.ActorTransportAdapter.AskTimeout
import akka.util.Timeout
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}

import java.net.URI
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}

object HelloWorldAkkaHttpServer {
  case class Name(name: String)
  case class Response(items: String)
}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit def listJsonWriter[T : JsonWriter]: RootJsonWriter[List[T]] = new  RootJsonWriter[List[T]] {
    def write(list: List[T]): JsArray = JsArray(list.map(_.toJson).toVector)
  }
  implicit val itemJsonFormat: JsonWriter[Item] = new JsonWriter[Item] {
    def write(item: Item): JsValue = {
      JsObject("name" -> JsString(item.name),
        "brand" -> JsString(item.brand),
        "price" -> JsString(item.price.toString()))
    }
  }
  implicit val nameFormat      = jsonFormat1(HelloWorldAkkaHttpServer.Name)
  implicit val greetingsFormat = jsonFormat1(HelloWorldAkkaHttpServer.Response)

  //custom formatter just for example
  implicit val uriFormat = new JsonFormat[java.net.URI] {
    override def write(obj: java.net.URI): spray.json.JsValue = JsString(obj.toString)
    override def read(json: JsValue): URI =
      json match {
        case JsString(url) => new URI(url)
        case _             => throw new RuntimeException("Parsing exception")
      }
  }

}

object HelloWorldAkkaHttpServerApp extends App {
  new HelloWorldAkkaHttpServer().start(9000)
}

/** Just to demonstrate how one can build akka-http based server with JsonSupport */
class HelloWorldAkkaHttpServer extends JsonSupport {
  private implicit val system = ActorSystem[Nothing](Behaviors.empty, "HelloWorldAkkaHttp")
  private implicit val scheduler = system.scheduler
  private implicit val timeout = 3.second

  def routes: Route = {
    path("catalog") {
      get {
        parameters("brand".as[String], "item".as[String]) { (brand, item) =>
          val listingFuture = system.receptionist.ask(
            (ref: ActorRef[Receptionist.Listing]) => Receptionist.find(ProductCatalog.ProductCatalogServiceKey, ref)
          )
          onSuccess(listingFuture) {
            listing: Receptionist.Listing =>
              val instances = listing.serviceInstances(ProductCatalog.ProductCatalogServiceKey)
              val productCatalog = instances.iterator.next()
              val items = productCatalog.ask(ref => GetItems(brand, List(item), ref)).mapTo[ProductCatalog.Items]
              onSuccess(items) {
                items: Items =>
                  complete(items.items.toJson(listJsonWriter(itemJsonFormat)))
              }
          }
        }
      }
    }
  }

  def start(port: Int) = {
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    Await.ready(system.whenTerminated, Duration.Inf)
  }

}
