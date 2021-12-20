package part2_event_sourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import scala.collection.mutable

object PersistentActorsExercise extends App {
  /*
    Persistent actor for a voting system
    Keep:
      - the citizens who voted
      - the poll : mapping between a candidate and the number of received votes so far
    The actor must be able to recover its state if it's shut down or restarted
  */
  case class Vote(citizenPID: String, candidate : String)
  case object PrintResult

  class VoteSystem extends PersistentActor with ActorLogging {
    var citizenList : Set[String] = Set()
    var voteCount : mutable.Map[String, Int] = mutable.Map()

    override def receiveRecover: Receive = {
      case Vote(citizenPID, candidate) if !citizenList(citizenPID) =>
        log.info(s"receiveRecover citizenPID - ${citizenPID}, candidate - ${candidate}")
        val vote = voteCount.getOrElse(candidate, 0)
        voteCount.put(candidate, vote + 1)
        citizenList = citizenList + citizenPID
    }

    override def receiveCommand: Receive = {
      case vote @ Vote(citizenPID, candidate) if !citizenList(citizenPID) =>
        persist(vote) { e =>
          log.info(s"receiveCommand citizenPID - ${e.citizenPID}, candidate - ${e.candidate}")
          val vote = voteCount.getOrElse(e.candidate, 0)
          voteCount.put(e.candidate, vote + 1)
          citizenList = citizenList + e.citizenPID
        }

      case PrintResult =>
        for (x <- voteCount) yield  {
          println(s"candidate ${x._1}, count ${x._2}")
        }
    }

    override def persistenceId: String = "VoteSystem"
  }

  val actorSystem = ActorSystem("ActorSystem")
  val voteSystem = actorSystem.actorOf(Props[VoteSystem])

  voteSystem ! Vote("12345", "Clinton")
  voteSystem ! Vote("1234568", "Clinton")

  voteSystem ! Vote("1234567", "Bush")

  voteSystem ! PrintResult
}
