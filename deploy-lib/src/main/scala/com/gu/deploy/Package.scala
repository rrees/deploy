package com.gu.deploy

case class Package(name: String, pkgRoles: Set[Role], pkgTypeName: String) {

  def mkAction(name: String): Action = pkgType.mkAction(name)

  lazy val pkgType = pkgTypeName match {
    case "jetty-webapp" => new JettyWebappPackageType(this)
    case "file" => new FilePackageType(this)
    case unknown => sys.error("Package type %s of package %s is unknown" format (unknown, name))
  }

  val roles = pkgRoles
}