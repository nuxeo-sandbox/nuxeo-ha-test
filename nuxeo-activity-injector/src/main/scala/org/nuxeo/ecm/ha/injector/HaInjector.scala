package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder._
import scala.util.Random



class HaInjector extends Simulation {



  val url = System.getProperty("url", "http://nuxeo.apps.io.nuxeo.com")
  val nbUsers = Integer.getInteger("users", 20)
  val myRamp = java.lang.Long.getLong("ramp", 30L)
  val myDuration = java.lang.Long.getLong("duration", 300L)
  
  val httpProtocol = http
    .baseURL(url)
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:35.0) Gecko/20100101 Firefox/35.0")

  val default = scenario("default").during(myDuration) {exec(CreateCheckDelete.scenario(2))}


  before {
    println("Simulation is about to start!")
 
  }

  after {
                      
    println("Simulation is finished!")
  }

  setUp(
    default.inject( rampUsers(nbUsers) over (myRamp seconds))    
  ).protocols(httpProtocol)

}
