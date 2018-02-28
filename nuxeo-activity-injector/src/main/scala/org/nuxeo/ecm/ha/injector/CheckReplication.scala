package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import java.util.concurrent.ConcurrentLinkedQueue

object CheckReplication {

  val scenario = (primary: String, secondary: String) => {
    group("Replication") {
      feed(HaFeeder.userFeeder)
        .exec { session =>
          session.set("description", null)
          session.set("primary", primary)
          session.set("secondary", secondary)
        }
        .exec(
          http("Step 0 - Check test workspace exists")
            .get(s"${primary}/nuxeo/api/v1/path/default-domain/workspaces/test")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userId}")
            .check(status.is(200))).exitHereIfFailed
        .exec(
          http("Step 1 - Create document")
            .post(s"${primary}/nuxeo/api/v1/path/default-domain/workspaces/test")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userId}")
            .body(StringBody("""{"entity-type": "document","type": "Note","title": "Test doc","name":"newdoc"}""")).asJSON
            .check(jsonPath("$.uid").saveAs("docId")))
        .tryMax(5) {
          pause(2 seconds)
            .exec(
              http("Step 1.1 - Check primary index")
                .post(s"${primary}/nuxeo/site/es/nuxeo/_search")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userId}")
                .body(StringBody("""{ "_source": ["ecm:uuid", "ecm:title"], "query": { "match": { "ecm:uuid": "${docId}" } } }""")).asJSON
                .check(jsonPath("$.hits.total").is("1")))
        }
        .pause(2 seconds)
        .tryMax(5) {
          pause(50 milliseconds)
            .exec(
              repeat(50) {
                doIf("${description.isUndefined()}") {
                  exec(
                    http("Step 2 - Retrieve document")
                      .get(s"${primary}/nuxeo/api/v1/id/" + "${docId}")
                      .headers(HaHeader.default)
                      .basicAuth("${userId}", "${userId}")
                      .check(status.not(500))
                      .check(status.not(504))
                      .check(jsonPath("$['properties']['dc:description']").optional.saveAs("description")))
                    .pause(2 seconds)
                }
              })
        }
        .tryMax(5) {
          pause(50 milliseconds)
            .exec(
              http("Step 3 - Check updated value")
                .get(s"${primary}/nuxeo/api/v1/id/" + "${docId}")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userId}")
                .check(status.not(500))
                .check(status.not(504))
                .check(jsonPath("$['properties']['dc:description']").is("updated")))
        }
        .pause(5 seconds)
        .tryMax(7) {
          pause(10 seconds)
            .exec(
              http("Step 4 - Check replication")
                .get(s"${secondary}/nuxeo/api/v1/id/" + "${docId}")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userId}")
                .check(status.not(500))
                .check(status.not(504))
                .check(jsonPath("$['properties']['dc:description']").is("updated")))
            .exec(
              http("Step 5 - Check replicated index")
                .post(s"${secondary}/nuxeo/site/es/nuxeo/_search")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userId}")
                .body(StringBody("""{ "_source": ["ecm:uuid", "ecm:title"], "query": { "match": { "ecm:uuid": "${docId}" } } }""")).asJSON
                .check(jsonPath("$.hits.total").is("1")))
        }
        .exec(
          http("Step 6 - Delete document")
            .delete(s"${primary}/nuxeo/api/v1/id/" + "${docId}")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userId}"))
        .pause(5 seconds)
        .tryMax(7) {
          pause(10 seconds)
            .exec(
              http("Step 7 - Check replication deleted")
                .get(s"${secondary}/nuxeo/api/v1/id/" + "${docId}")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userId}")
                .check(status.not(200)))
            .exec(
              http("Step 8 - Check index updated")
                .post(s"${secondary}/nuxeo/site/es/nuxeo/_search")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userId}")
                .body(StringBody("""{ "_source": ["ecm:uuid", "ecm:title"], "query": { "match": { "ecm:uuid": "${docId}" } } }""")).asJSON
                .check(jsonPath("$.hits.total").is("0")))
        }
    }
  }

}
