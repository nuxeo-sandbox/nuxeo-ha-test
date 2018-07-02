package org.nuxeo.ecm.ha.injector

import io.gatling.commons.util.LongCounter

object HaFeeder {

  val userPass = sys.props.getOrElse("password", "Administrator")
  val userName = sys.props.getOrElse("user", "Administrator")
  println(s"User: ${userName}:${userPass}")
  val userFeeder = Iterator.continually(Map("userId" -> (s"${userName}"), "userPass" -> (s"${userPass}")))

  val idRange = new LongCounter()
  val idFeeder = Iterator.continually(Map("entryId" -> idRange.incrementAndGet(), "userId" -> (s"${userName}"), "userPass" -> (s"${userPass}")))
}

object HaHeader {

  val default = Map(
    "Content-Type" -> "application/json+nxrequest; charset=UTF-8",
    "X-NXCoreIoMarshaller" -> "true",
    "X-NXDocumentProperties" -> "dublincore, file",
    "X-NXRepository" -> "default")

}

