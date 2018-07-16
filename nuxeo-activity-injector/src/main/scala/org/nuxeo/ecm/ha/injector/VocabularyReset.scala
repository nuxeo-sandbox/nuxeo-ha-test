package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

object VocabularyReset {

  val vocabulary = sys.props.getOrElse("vocabulary", "retention")

  val scenario = (primary: String, secondary: String) => {
    group("Preconditions") {
      feed(HaFeeder.idFeeder)
        .exec { session =>
          session.remove("foundId")
            .set("primary", primary)
            .set("secondary", secondary)
            .set("vocabulary", s"${vocabulary}")
        }
        .doIf("${vocabFound.isUndefined()}") {
          exec(
            http("Step 0.0 - Check Vocabulary")
              .get(s"${primary}/nuxeo/api/v1/" + "directory/${vocabulary}/0")
              .headers(HaHeader.default)
              .basicAuth("${userId}", "${userPass}")
              .check(status.is(200))).exitHereIfFailed
            .exec { session => session.set("vocabFound", true) }
        }
    }
      .group("Vocabulary Reset") {
        tryMax(1) {
          exec(
            http("Step 1.0 - Get Entry")
              .get(s"${primary}/nuxeo/api/v1/" + "directory/${vocabulary}/${entryId}")
              .headers(HaHeader.default)
              .basicAuth("${userId}", "${userPass}")
              .check(jsonPath("$.properties.id").optional.saveAs("foundId")))
        }
          .doIfOrElse("${foundId.isUndefined()}")(
            exec(
              http("Step 1.1.1 - Create Entry")
                .post(s"${primary}/nuxeo/api/v1/" + "directory/${vocabulary}")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userPass}")
                .body(StringBody("""
                {
                  "entity-type": "directoryEntry",
                  "directoryName": "${vocabulary}",
                  "properties": {
                    "id": "${entryId}",
                    "obsolete": "0",
                    "ordering": "1",
                    "label": "label.directories.${vocabulary}.${entryId}"
                  }
                }""")).asJSON))(
              exec(
                http("Step 1.1.2 - Update Entry")
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
                    "ordering": "1",
                    "label": "label.directories.${vocabulary}.${entryId}"
                  }
                }""")).asJSON))
      }
  }

}