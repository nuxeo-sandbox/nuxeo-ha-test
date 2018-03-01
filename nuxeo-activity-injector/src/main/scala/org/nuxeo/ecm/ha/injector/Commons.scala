package org.nuxeo.ecm.ha.injector

import io.gatling.core.Predef._

object HaFeeder {  

  val userFeeder = Iterator.continually(Map("userId" -> ("Administrator")))
}

object HaHeader {

  val default = Map(
    "Content-Type" -> "application/json+nxrequest; charset=UTF-8",
    "X-NXCoreIoMarshaller" -> "true",
    "X-NXDocumentProperties" -> "dublincore, file",
    "X-NXRepository" -> "default")

}
