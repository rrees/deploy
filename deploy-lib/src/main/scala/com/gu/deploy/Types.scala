package com.gu.deploy

import tasks._

trait PackageType {
  def name: String
  def pkg: Package

  def mkAction(actionName: String): Action = {
    if (actions.isDefinedAt(actionName)) {
      new Action {
        def resolve(host: Host) = actions(actionName)(host)
        def roles = pkg.roles
        def description = pkg.name + "." + actionName
        override def toString = "action " + description
      }
    } else {
      sys.error("Action %s is not supported on package %s of type %s" format (actionName, pkg.name, name))
    }
  }

  type ActionDefinition = PartialFunction[String, Host => List[Task]]
  val actions: ActionDefinition
}

case class JettyWebappPackageType(pkg: Package) extends PackageType {
  val name = "jetty-webapp"

  val actions: ActionDefinition = {
    case "deploy" => { host => List(
        BlockFirewall(),
        CopyFile("packages/%s" format pkg.name, "/jetty-apps/%s/" format pkg.name),
        RestartAndWait(),
        UnblockFirewall()
      )
    }
  }

  def deployWebapp(packageName: String, host: Host) = List(
        BlockFirewall(),
        CopyFile("packages/%s" format packageName, "/jetty-apps/%s/" format packageName),
        RestartAndWait(),
        UnblockFirewall()
      )
}


case class FilePackageType(pkg: Package) extends PackageType {
  val name = "file"

  val actions: ActionDefinition = {
    case "deploy" => host => List(CopyFile("packages/%s" format (pkg.name), "/"))
  }

}