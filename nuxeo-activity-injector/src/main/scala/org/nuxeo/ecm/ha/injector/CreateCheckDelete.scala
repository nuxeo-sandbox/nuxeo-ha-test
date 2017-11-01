package org.nuxeo.ecm.ha.injector


import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

object CreateCheckDelete {

  
  val scenario = (asyncPauseTime: Integer) => {
    group("CreateCheckDelete") {
      feed(HaFeeder.userFeeder)        
        .exec{ session =>
          session.set("description",null)
        }
        .exec(
          http("Step 1 - Create document")
            .post("/nuxeo/api/v1/path/default-domain/workspaces/test")
            .headers(HaHeader.default)
            .basicAuth("${userId}","${userId}")            
            .body(StringBody("""{"entity-type": "document","type": "Note","title": "Test doc","name":"newdoc"}""")).asJSON
            .check(jsonPath("$.uid").saveAs("docId"))

        )        
        .pause(2 seconds)
        .repeat(50) {
          doIf("${description.isUndefined()}") {
            exec(
              http("Step 2 - Retrieve document")
                .get("/nuxeo/api/v1/id/${docId}")
                .headers(HaHeader.default)
                .basicAuth("${userId}","${userId}")
                .check(jsonPath("$['properties']['dc:description']").optional.saveAs("description"))
            )   
            .pause(2 seconds)            
          }
        }
        .exec(
          http("Step 3 - Check updated value")
            .get("/nuxeo/api/v1/id/${docId}")
            .headers(HaHeader.default)
            .basicAuth("${userId}","${userId}")
            .check(jsonPath("$['properties']['dc:description']").is("updated"))
        )                    
        .exec(
          http("Step 4 - Delete document")
            .delete("/nuxeo/api/v1/id/${docId}")
            .headers(HaHeader.default)
            .basicAuth("${userId}","${userId}")                        
          )
        
    }
  }

}
