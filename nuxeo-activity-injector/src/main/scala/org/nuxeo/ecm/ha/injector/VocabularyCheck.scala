package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

object VocabularyCheck {

  val vocabulary = sys.props.getOrElse("vocabulary", "retention")

  val scenario = (primary: String, secondary: String) => {
    group("Preconditions") {
      feed(HaFeeder.idFeeder)
        .exec { session =>
          session.remove("ordering")
            .set("primary", primary)
            .set("secondary", secondary)
            .set("vocabulary", s"${vocabulary}")
        }
        .exec(
          http("Step 0.0 - Check Vocabulary")
            .get(s"${primary}/nuxeo/api/v1/" + "directory/${vocabulary}/0")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(200))).exitHereIfFailed
    }
      .group("Vocabulary Update") {
        exec(
          http("Step 1.0 - Check Entry")
            .get(s"${primary}/nuxeo/api/v1/" + "directory/${vocabulary}/${entryId}")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(200))
            .check(jsonPath("$.properties.ordering").saveAs("ordering")))
          .exec(
            http("Step 1.1 - Update Entry")
              .put(s"${primary}/nuxeo/api/v1/" + "directory/${vocabulary}/${entryId}")
              .headers(HaHeader.default)
              .basicAuth("${userId}", "${userPass}")
              .body(StringBody("""
                {
                  "entity-type": "directoryEntry",
                  "directoryName": "${vocabulary}",
                  "properties": {
                    "id": "${entryId}",
                    "obsolete": "0",
                    "ordering": "${ordering}0",
                    "label": "label.directories.${vocabulary}.${entryId}"
                  }
                }""")).asJSON
              .check(status.is(200)))
      }
      .doIf("${secondary.exists()}") {
        group("Check Replicated Vocabulary") {
          pause(5 seconds)
            .tryMax(7) {
              pause(10 seconds)
                .exec(
                  http("Step 2.0 - Check Replicated Entry")
                    .get(s"${secondary}/nuxeo/api/v1/" + "directory/${vocabulary}/${entryId}")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .check(status.not(500))
                    .check(status.not(504))
                    .check(jsonPath("$.properties.ordering").is("${ordering}0")))
            }
        }
      }
  }

}