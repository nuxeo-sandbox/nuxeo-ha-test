package org.nuxeo.ecm.ha.injector

import io.gatling.commons.util.LongCounter

object HaFeeder {

  val userFeeder = Iterator.continually(Map("userId" -> ("Administrator")))

  val idRange = new LongCounter()
  val idFeeder = Iterator.continually(Map("entryId" -> idRange.incrementAndGet(), "userId" -> ("Administrator")))
}

object HaHeader {

  val default = Map(
    "Content-Type" -> "application/json+nxrequest; charset=UTF-8",
    "X-NXCoreIoMarshaller" -> "true",
    "X-NXDocumentProperties" -> "dublincore, file",
    "X-NXRepository" -> "default")

}

