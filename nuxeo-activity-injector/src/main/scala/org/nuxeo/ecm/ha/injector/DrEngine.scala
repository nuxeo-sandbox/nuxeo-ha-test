package org.nuxeo.ecm.ha.injector

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object DrEngine extends App {
  val props = new GatlingPropertiesBuilder
  props.dataDirectory("jar")
  props.simulationClass(classOf[DrInjector].getName)

  Gatling.fromMap(props.build)
  sys.exit()
}