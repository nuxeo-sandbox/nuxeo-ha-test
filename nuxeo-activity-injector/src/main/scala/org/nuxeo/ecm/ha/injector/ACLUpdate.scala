package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

object ACLUpdate {

  val uidFeeder = csv(sys.props.getOrElse("csv", "uid_export.csv")).circular

  val scenario = (primary: String, secondary: String) => {
    group("Preconditions") {
      feed(uidFeeder).feed(HaFeeder.userFeeder)
        .exec { session =>
          session.set("primary", primary)
            .set("secondary", secondary)
        }
        .exec(
          http("Step 0.0 - Check test workspace exists")
            .get(s"${primary}/nuxeo/api/v1/path/default-domain/workspaces/test")
            .headers(HaHeader.default)
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(200))).exitHereIfFailed
    }
      .group("Update ACE") {
        exec(
          http("Step 1.0 - Check Document Exists")
            .get(s"${primary}/nuxeo/api/v1/" + "id/${docId}")
            .headers(HaHeader.default)
            .header("enrichers.document", "document,acls")
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(200)))
          .exec(
            http("Step 1.1 - Update Access Control Entry")
              .post(s"${primary}/nuxeo/site/automation/Document.AddPermission")
              .headers(HaHeader.default)
              .header("enrichers.document", "document,acls")
              .basicAuth("${userId}", "${userPass}")
              .body(StringBody("""
                {"params":
                  {"permission":"Everything","acl":"local","username":"members"},
                  "input":"doc:${docId}","context":{}}
                 """)).asJSON
              .check(status.is(200)))
          .exec(
            http("Step 1.2 - Check Updated Access Control Entry")
              .get(s"${primary}/nuxeo/api/v1/" + "id/${docId}")
              .headers(HaHeader.default)
              .header("enrichers.document", "document,acls")
              .basicAuth("${userId}", "${userPass}")
              .check(status.not(500))
              .check(status.not(504))
              .check(jsonPath("$.contextParameters.acls[*].aces[*].id").is("members:Everything:true:Administrator::")))
      }
      .doIf("${secondary.exists()}") {
        group("Check Replicated ACE") {
          pause(5 seconds)
            .tryMax(7) {
              pause(10 seconds)
                .exec(
                  http("Step 2.0 - Check Replicated Access Control Entry")
                    .get(s"${secondary}/nuxeo/api/v1/" + "id/${docId}")
                    .headers(HaHeader.default)
                    .header("enrichers.document", "document,acls")
                    .basicAuth("${userId}", "${userPass}")
                    .check(status.not(500))
                    .check(status.not(504))
                    .check(jsonPath("$.contextParameters.acls[*].aces[*].id").is("members:Everything:true:Administrator::")))
            }
        }
      }
      .group("Remove ACE") {
        exec(
          http("Step 3.0 - Check Document Exists")
            .get(s"${primary}/nuxeo/api/v1/" + "id/${docId}")
            .headers(HaHeader.default)
            .header("enrichers.document", "document,acls")
            .basicAuth("${userId}", "${userPass}")
            .check(status.is(200)))
          .exec(
            http("Step 3.1 - Remove Access Control Entry")
              .post(s"${primary}/nuxeo/site/automation/Document.RemovePermission")
              .headers(HaHeader.default)
              .header("enrichers.document", "document,acls")
              .basicAuth("${userId}", "${userPass}")
              .body(StringBody("""
                {"params":
                  {"id":"members:Everything:true:Administrator::"},
                  "input":"doc:${docId}","context":{}}
                 """)).asJSON
              .check(status.is(200)))
          .exec(
            http("Step 3.2 - Check Removed Access Control Entry")
              .get(s"${primary}/nuxeo/api/v1/" + "id/${docId}")
              .headers(HaHeader.default)
              .header("enrichers.document", "document,acls")
              .basicAuth("${userId}", "${userPass}")
              .check(status.not(500))
              .check(status.not(504))
              .check(jsonPath("$.contextParameters.acls[*].aces[*].id").not("members:Everything:true:Administrator::")))
      }
      .doIf("${secondary.exists()}") {
        group("Check Removed ACE") {
          pause(5 seconds)
            .tryMax(7) {
              pause(10 seconds)
                .exec(
                  http("Step 4.0 - Check Entry Removed")
                    .get(s"${secondary}/nuxeo/api/v1/" + "id/${docId}")
                    .headers(HaHeader.default)
                    .header("enrichers.document", "document,acls")
                    .basicAuth("${userId}", "${userPass}")
                    .check(status.not(500))
                    .check(status.not(504))
                    .check(jsonPath("$.contextParameters.acls[*].aces[*].id").not("members:Everything:true:Administrator::")))
            }
        }
      }
  }

}