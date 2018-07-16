package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

object ColdReplicationCheck {

  val uidFeeder = csv(sys.props.getOrElse("csv", "uid_export.csv")).circular
  val indexCheck = (sys.props.getOrElse("index", "true") equalsIgnoreCase "true")

  val scenario = (primary: String, secondary: String) => {
    group("Preconditions") {
      feed(uidFeeder).feed(HaFeeder.userFeeder)
        .exec { session =>
          session.set("primary", primary)
        }
        .exec(
          http("Step 0.0 - Check test workspace exists")
            .get(s"${primary}/nuxeo/api/v1/path/default-domain/workspaces/test")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(200))).exitHereIfFailed
    }
      .group("Document Retrieval") {
        exec(
          http("Step 1.1 - Check Document Exists")
            .get(s"${primary}/nuxeo/api/v1/" + "id/${docId}")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userPass}")
            .check(status.not(500))
            .check(status.not(504))
            .check(jsonPath("$.properties['file:content'].digest").optional.saveAs("digest")))
          .doIf("${digest.exists()}") {
            exec(
              http("Step 1.2 - Verify Binary Presence")
                .post(s"${primary}/nuxeo/site/automation/Blob.VerifyBinaryHash")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userPass}")
                .body(StringBody("""{"params":{"digest":"${digest}"},"context":{}}""")).asJSON
                .check(status.is(200))
                .check(jsonPath("$.value").is("${digest}"))
                .check(jsonPath("$.value").is("${hash}")))
          }
          .doIf(indexCheck) {
            tryMax(5) {
              pause(2 seconds)
                .exec(
                  http("Step 1.3 - Check Index")
                    .post(s"${primary}/nuxeo/site/es/nuxeo/_search")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .body(StringBody("""{ "_source": ["ecm:uuid", "ecm:title"], "query": { "match": { "ecm:uuid": "${docId}" } } }""")).asJSON
                    .check(jsonPath("$.hits.total").is("1")))
            }
          }
      }
  }

}
