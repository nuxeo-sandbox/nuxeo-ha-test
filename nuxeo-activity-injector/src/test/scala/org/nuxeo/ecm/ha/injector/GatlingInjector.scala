
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random



class HaInjector extends Simulation {



  val url = System.getProperty("url", "http://nuxeo.apps.io.nuxeo.com")
  val nbUsers = Integer.getInteger("users", 10)
  val myRamp = java.lang.Long.getLong("ramp", 2L)
  val myDuration = java.lang.Long.getLong("duration", 180L)
  val thinkTime = Integer.getInteger("pause", 1)

  val scn1Percentage = Integer.getInteger("scn1", 100)
  
  def getNbUsers(percentage:Int ):Int = {
    (nbUsers.intValue * percentage.intValue / 100.0).ceil.toInt
  }

  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")
    .userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:35.0) Gecko/20100101 Firefox/35.0")

  val default = scenario("default").during(myDuration) {exec(CreateCheckDelete.scenario(thinkTime))}

  setUp(
    default.inject((rampUsers(getNbUsers(scn1Percentage.intValue)).over(myRamp)))    
  ).protocols(httpProtocol)

}
