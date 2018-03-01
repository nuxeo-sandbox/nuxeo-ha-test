package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder._
import scala.util.Random

class HaInjector extends Simulation {

  val primary = System.getProperty("primary", "http://nuxeo.apps.io.nuxeo.com")
  val secondary = System.getProperty("secondary", null)
  val nbUsers = Integer.getInteger("users", 20)
  val myRamp = java.lang.Long.getLong("ramp", 30L)
  val myDuration = java.lang.Long.getLong("duration", 300L)

  val httpProtocol = http
    .baseURL(primary)
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:35.0) Gecko/20100101 Firefox/35.0")

  val default = scenario("HA/DR").during(myDuration) { exec(CreateCheckDelete.scenario(primary, secondary)) }

  before {
    println("Simulation is about to start!")

  }

  after {

    println("Simulation is finished!")
  }

  setUp(
    default.inject(rampUsers(nbUsers) over (myRamp seconds))).protocols(httpProtocol)

}
