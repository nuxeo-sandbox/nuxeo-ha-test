package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import io.gatling.core.session.el._
import io.gatling.core.session._
import io.gatling.commons.validation._

import scala.concurrent.duration._

object CreateCheckDelete {

  val indexCheck = (sys.props.getOrElse("index", "true") equalsIgnoreCase "true")
  val deleteDoc = (sys.props.getOrElse("delete", "true") equalsIgnoreCase "true")
  val checkUpdate = (sys.props.getOrElse("checkUpdate", "true") equalsIgnoreCase "true")

  val scenario = (primary: String, secondary: String) => {
    group("Preconditions") {
      feed(HaFeeder.userFeeder)
        .exec { session =>
          session.set("description", null)
            .set("primary", primary)
            .set("secondary", secondary)
        }
        .exec(
          http("Step 0.0 - Check test workspace exists")
            .get(s"${primary}/nuxeo/api/v1/path/default-domain/workspaces/test")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(200))).exitHereIfFailed
    }
      .group("Document Creation") {
        exec(
          http("Step 1.1 - Create Upload Batch")
            .post(s"${primary}/nuxeo/api/v1/upload/")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(201))
            .check(jsonPath("$.batchId").saveAs("batchId")))
          .exec(
            http("Step 1.2 - Upload Data")
              .post(s"${primary}/nuxeo/api/v1/" + "upload/${batchId}/0")
              .headers(HaHeader.default)
              .header("X-File-Name", "note-${batchId}.txt")
              .header("X-File-Type", "text/plain")
              .basicAuth("${userId}", "${userPass}")
              .body(StringBody("This is a note for batch ${batchId}"))
              .check(status.is(201)))
          .exec(
            http("Step 1.3 - Create Document")
              .post(s"${primary}/nuxeo/api/v1/path/default-domain/workspaces/test")
              .headers(HaHeader.default)
              .basicAuth("${userId}", "${userPass}")
              .body(StringBody("""
                {"entity-type":"document",
                "type":"File",
                "name":"newdoc",
                "properties":{
                  "dc:title":"note-${batchId}",
                  "dc:source":"2",
                  "file:content": {
                    "upload-batch":"${batchId}",
                    "upload-fileId":"0"
                }}}""")).asJSON
              .check(jsonPath("$.uid").saveAs("docId")))
          .doIf(indexCheck) {
            tryMax(5) {
              pause(2 seconds)
                .exec(
                  http("Step 1.4 - Check primary index")
                    .post(s"${primary}/nuxeo/site/es/nuxeo/_search")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .body(StringBody("""{ "_source": ["ecm:uuid", "ecm:title"], "query": { "match": { "ecm:uuid": "${docId}" } } }""")).asJSON
                    .check(jsonPath("$.hits.total").is("1")))
            }
          }
      }
      .doIf(checkUpdate) {
        group("Asynchronous Update") {
          pause(1 seconds)
            .tryMax(10) {
              pause(1 seconds)
                .exec(
                  repeat(10) {
                    doIf("${description.isUndefined()}") {
                      exec(
                        http("Step 2.0 - Retrieve Async Work")
                          .get(s"${primary}/nuxeo/api/v1/" + "id/${docId}")
                          .headers(HaHeader.default)
                          .basicAuth("${userId}", "${userPass}")
                          .check(status.not(500))
                          .check(status.not(504))
                          .check(jsonPath("$['properties']['dc:description']").optional.saveAs("description")))
                        .pause(2 seconds)
                    }
                  })
            }
            .tryMax(5) {
              pause(1 seconds)
                .exec(
                  http("Step 2.1 - Check Metadata")
                    .get(s"${primary}/nuxeo/api/v1/" + "id/${docId}")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .check(status.not(500))
                    .check(status.not(504))
                    .check(jsonPath("$.properties['file:content'].digest").saveAs("digest"))
                    .check(jsonPath("$['properties']['dc:description']").is("updated")))
                .exec(
                  http("Step 2.2 - Verify Binary Presence")
                    .post(s"${primary}/nuxeo/site/automation/Blob.VerifyBinaryHash")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .body(StringBody("""{"params":{"digest":"${digest}"},"context":{}}""")).asJSON
                    .check(status.is(200))
                    .check(jsonPath("$.value").is("${digest}")))

            }
        }
      }
      .doIf("${secondary.exists()}") {
        group("Check Replicated Document") {
          pause(5 seconds)
            .tryMax(7) {
              pause(10 seconds)
                .exec(
                  http("Step 4.0 - Check Replicated Document")
                    .get(s"${secondary}/nuxeo/api/v1/" + "id/${docId}")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .check(status.not(500))
                    .check(status.not(504))
                    .check(jsonPath("$.properties['file:content'].digest").is("${digest}"))
                    .check(jsonPath("$['properties']['dc:description']").is("updated")))
                .exec(
                  http("Step 4.1 - Check Replicated Index")
                    .post(s"${secondary}/nuxeo/site/es/nuxeo/_search")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .body(StringBody("""{ "_source": ["ecm:uuid", "ecm:title"], "query": { "match": { "ecm:uuid": "${docId}" } } }""")).asJSON
                    .check(jsonPath("$.hits.total").is("1")))
                .exec(
                  http("Step 4.2 - Verify Binary Presence")
                    .post(s"${secondary}/nuxeo/site/automation/Blob.VerifyBinaryHash")
                    .headers(HaHeader.default)
                    .basicAuth("${userId}", "${userPass}")
                    .body(StringBody("""{"params":{"digest":"${digest}"},"context":{}}""")).asJSON
                    .check(status.is(200))
                    .check(jsonPath("$.value").is("${digest}")))

            }
        }
      }
      .doIf(deleteDoc) {
        group("Document Cleanup") {
          tryMax(1) {
            exec(
              http("Step 5.0 - Delete document")
                .delete(s"${primary}/nuxeo/api/v1/id/" + "${docId}")
                .headers(HaHeader.default)
                .basicAuth("${userId}", "${userPass}")
                .check(status.is(204)))
          }
            .doIf("${secondary.exists()}") {
              pause(5 seconds)
                .tryMax(7) {
                  pause(10 seconds)
                    .exec(
                      http("Step 5.1 - Check replication deleted")
                        .get(s"${secondary}/nuxeo/api/v1/" + "id/${docId}")
                        .headers(HaHeader.default)
                        .basicAuth("${userId}", "${userPass}")
                        .check(status.not(200)))
                    .doIf(indexCheck) {
                      exec(http("Step 5.2 - Check index updated")
                        .post(s"${secondary}/nuxeo/site/es/nuxeo/_search")
                        .headers(HaHeader.default)
                        .basicAuth("${userId}", "${userPass}")
                        .body(StringBody("""{ "_source": ["ecm:uuid", "ecm:title"], "query": { "match": { "ecm:uuid": "${docId}" } } }""")).asJSON
                        .check(jsonPath("$.hits.total").is("0")))
                    }
                }
            }
        }
      }
  }

}
