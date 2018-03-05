package org.nuxeo.ecm.ha.injector

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder
import java.io.File

object Engine extends App {

  val props = new GatlingPropertiesBuilder
  props.dataDirectory("jar")
  props.simulationClass(classOf[HaInjector].getName)

  Gatling.fromMap(props.build)
  sys.exit()

}