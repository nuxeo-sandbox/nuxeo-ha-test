package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder._
import scala.util.Random

class HaInjector extends Simulation {

  val primary = sys.props.getOrElse("primary", "https://nuxeo.apps.prod.nuxeo.io")
  val secondary = sys.props.getOrElse("secondary", null)
  val nbUsers = Integer.getInteger("users", 20)
  val myRamp = java.lang.Long.getLong("ramp", 30L)
  val myDuration = java.lang.Long.getLong("duration", 300L)
  val myEntries = Integer.getInteger("entries", 3000)
  val simulation = sys.props.getOrElse("scenario", "ha")
  println(s"URL: ${primary}; Simulation: ${simulation}")

  val httpProtocol = http
    .baseURL(primary)
    .acceptEncodingHeader("gzip,deflate")
    .userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:35.0) Gecko/20100101 Firefox/35.0")

  val vocabRounds = (myEntries.div(nbUsers)).toInt

  val selected = simulation match {
    case "ace" =>
      val chain = scenario("ACE Replication Check").during(myDuration) { exec(ACLUpdate.scenario(primary, secondary)) }
      setUp(chain.inject(rampUsers(nbUsers) over (myRamp seconds)))
    case "ha" =>
      val chain = scenario("HA/DR").during(myDuration) { exec(CreateCheckDelete.scenario(primary, secondary)) }
      setUp(chain.inject(rampUsers(nbUsers) over (myRamp seconds)))
    case "cold" =>
      val chain = scenario("Cold Replication Check").during(myDuration) { exec(ColdReplicationCheck.scenario(primary, secondary)) }
      setUp(chain.inject(rampUsers(nbUsers) over (myRamp seconds)))
    case "vcheck" =>
      val chain = scenario("Vocabulary Check").repeat(vocabRounds) { exec(VocabularyCheck.scenario(primary, secondary)) }
      setUp(chain.inject(rampUsers(nbUsers) over (myRamp seconds)))
    case "vreset" =>
      val chain = scenario("Vocabulary Reset").repeat(vocabRounds) { exec(VocabularyReset.scenario(primary, secondary)) }
      setUp(chain.inject(rampUsers(nbUsers) over (myRamp seconds)))
    case _ =>
      throw new IllegalArgumentException("No such scenario: " + simulation)
  }

  before {
    println("Simulation is about to start!")

  }

  after {

    println("Simulation is finished!")
  }

  selected.protocols(httpProtocol)

}
