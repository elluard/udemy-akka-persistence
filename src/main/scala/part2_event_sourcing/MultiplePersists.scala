package part2_event_sourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object MultiplePersists extends App {
  /*
    Diligent accountant: with every invoice, will persist TWO events
    - a tax record for the fiscal authority
    - an invoice record for personal logs or some auditing authority
   */

  // COMMANDS
  case class Invoice(recipient: String, date : Date, mount : Int)

  //EVENT
  case class TaxRecord(taxId: String, recordId: Int, date : Date, totalAmount: Int)
  case class InvoiceRecord(invoiceRecordId: Int, recipient: String, date : Date, mount : Int)

  object DiligentAccountant {
    def props(taxId: String, taxAuthority: ActorRef) = Props(new DiligentAccountant(taxId, taxAuthority))
  }

  class DiligentAccountant(taxId: String, taxAuthority: ActorRef) extends PersistentActor with ActorLogging {
    override def receiveRecover: Receive = {
      case event => log.info(s"Recovered: $event")
    }

    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
    }

    override def persistenceId: String = "diligent-accountant"
  }

  class TaxAuthority extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Received $message")
    }
  }

  val system = ActorSystem("MultiplePersistsDemo")
  val taxAuthority = system.actorOf(Props[TaxAuthority], "HMRC")
  val accountant = system.actorOf(DiligentAccountant.props("UK3522_345", taxAuthority))
}
