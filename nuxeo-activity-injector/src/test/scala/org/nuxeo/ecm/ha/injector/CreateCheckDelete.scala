import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

object CreateCheckDelete {

  def computePause(loopIndex: Int): Duration = loopIndex seconds


  val scenario = (asyncPauseTime: Integer) => {
    group("CreateCheckDelete") {
      feed(HaFeeder.userFeeder)        
        .exec(
          http("Step 1 - Create document")
            .post("/nuxeo/api/v1/path/default-domain/workspaces/test")
            .headers(HaHeader.default)
            .basicAuth("${userId}","${userId}")            
            .body(StringBody("""{"entity-type": "document","type": "Note","title": "Test doc","name":"newdoc"}""")).asJSON
            .check(jsonPath("$.uid").saveAs("docId"))
        )
        .pause(8 seconds) // Pause for the async worker to update the proerty
        .tryMax(4, "loopIndex") { // Loop for additional 10s max
          pause(4 seconds)                            
          .exec(
            http("Step 2 - Retrieve document")
              .get("/nuxeo/api/v1/id/${docId}")
              .headers(HaHeader.default)
              .check(jsonPath("$['properties']['dc:description']").is("updated"))
              .basicAuth("${userId}","${userId}")              
          )
          
        }
        /*.exitBlockOnFail{
          pause(1)
          .exec( http("Step 3 - Delete document")
            .delete("/nuxeo/api/v1/id/${docId}")
            .headers(HaHeader.default)
            .basicAuth("${userId}","${userId}")                        
          )
        }  */      
        /*.pause(5)
        .exec(
          http("Step 3 - Delete document")
            .delete("/nuxeo/api/v1/id/${docId}")
            .headers(HaHeader.default)
            .basicAuth("${userId}","${userId}")                        
          )*/
        
    }
  }

}
