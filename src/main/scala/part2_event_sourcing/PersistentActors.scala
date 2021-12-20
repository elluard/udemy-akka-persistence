package part2_event_sourcing

import akka.actor.FSM.Shutdown
import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.persistence.PersistentActor
import com.typesafe.config.ConfigFactory

import java.util.Date

object PersistentActors extends App {
  case class Invoice(recipient: String, date : Date, amount : Int)
  case class InvoiceBulk(invoices: List[Invoice])
  case class InvoiceRecorded(id : Int, recipient : String, date : Date, amount : Int)
  case object Shutdown


  class Accountant extends PersistentActor with ActorLogging {
    var latestInvoiceId = 0
    var totalAmount = 0
    //should be unique per each actor
    override def persistenceId: String = "simple-accountant"

    /**
      * The "normal" receive method
      * @return
      */
    override def receiveCommand: Receive = {
      case Invoice(recipient, date, amount) =>
        log.info(s"Receive invoice for amount $amount")
        val event = InvoiceRecorded(latestInvoiceId, recipient, date, amount)
        persist(event) { e =>
          // update state
          latestInvoiceId += 1
          totalAmount += amount
          log.info(s"Persisted $e as invoice number #${e.id}, for totalAmount $totalAmount")
        }

      case InvoiceBulk(invoices) =>
        //1. create events (plural)
        //2. persist all the events
        //3. update the actor state when each event is persisted
        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map { pair =>
          val id = pair._2
          val invoice = pair._1

          InvoiceRecorded(id, invoice.recipient, invoice.date, invoice.amount)
        }
        persistAll(events) { e =>
          latestInvoiceId += 1
          totalAmount += e.amount
          log.info(s"Persisted SINGLE $e as invoice number #${e.id}, for totalAmount $totalAmount")
        }

      case Shutdown =>
        context.stop(self)
    }

    /**
      * Handler that will be  called on recovery
      * @return
      */
    override def receiveRecover: Receive = {
      /*
        best practice : follow the logic in the persist steps of receiveCommand
       */
      case InvoiceRecorded(id, _, _ , amount) =>
        log.info(s"receiveRecover id : $id, amount $amount")
        latestInvoiceId = id
        totalAmount += amount
    }

    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Fail to persist $event because of $cause")
      super.onPersistFailure(cause, event, seqNr)
    }

    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Persist rejected $event because of $cause")
      super.onPersistRejected(cause, event, seqNr)
    }
  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant])
  for (i <- 1 to 10) {
    accountant ! Invoice("The sofa company", new Date, i * 1000)
  }

  //NEVER EVER CALL PERSIST OR PERSIST ALL FROM FUTURES
  val newInvoices = for (i <- 1 to 5) yield Invoice("The awesome chairs", new Date, i * 2000)
  accountant ! InvoiceBulk(newInvoices.toList)


  /**
    * Shutdown of persistent actors
    *
    * Best Practice : define your own "shutdown" messages
    */
//  accountant ! PoisonPill
  accountant ! Shutdown
}
