package part3_stores_serialization

import akka.actor.{ ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import part3_stores_serialization.LocalStores.SimplePersistentActor

object Cassandra extends App {
  val localStoresActorSystem = ActorSystem("localStoresSystem", ConfigFactory.load().getConfig("cassandraDemo"))
  val persistentActor = localStoresActorSystem.actorOf(Props[SimplePersistentActor], "simplePersistentActor")

//  for(i <- 1 to 10) {
//    persistentActor ! s"I love Akka [${i}]"
//  }
//
//  persistentActor ! "print"
//  persistentActor ! "snap"
//
//  for(i <- 11 to 20) {
//    persistentActor ! s"I love Akka [${i}]"
//  }
}
