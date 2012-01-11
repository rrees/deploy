package magenta

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import tasks._
import java.io.File
import net.liftweb.util.TimeHelpers._
import net.liftweb.json.Implicits._
import net.liftweb.json.JsonAST.{JArray, JString}


class PackageTypeTest extends FlatSpec with ShouldMatchers {

  "jetty web app package type" should "have a deploy action" in {
    val p = Package("webapp", Set.empty, Map.empty, "jetty-webapp", new File("/tmp/packages/webapp"))

    val jetty = new JettyWebappPackageType(p)
    val host = Host("host_name")

    jetty.actions("deploy")(host) should be (List(
      BlockFirewall(host as "jetty"),
      CopyFile(host as "jetty", "/tmp/packages/webapp/*", "/jetty-apps/webapp/"),
      Restart(host as "jetty", "webapp"),
      WaitForPort(host, "8080", 20 seconds),
      CheckUrls(host, "8080", List(), 20 seconds),
      UnblockFirewall(host as "jetty")
    ))
  }

  it should "allow port to be overriden" in {
    val basic = Package("webapp", Set.empty, Map.empty, "jetty-webapp", new File("/tmp/packages/webapp"))
    basic.data("port") should be (JString("8080"))
    basic.stringData("port") should be ("8080")

    val overridden = Package("webapp", Set.empty, Map("port" -> "80"), "jetty-webapp", new File("/tmp/packages/webapp"))
    overridden.data("port") should be (JString("80"))
    overridden.stringData("port") should be ("80")
  }

  it should "allow urls to check after deploy" in {
    val urls = JArray(List("/test", "/xx"))

    val basic = Package("webapp", Set.empty, Map("healthcheck_paths" -> urls), "jetty-webapp", new File("/tmp/packages/webapp"))
    basic.data("healthcheck_paths") should be (urls)
  }

  it should "check urls when specified" in {
    val urls = List("/test", "/xx")
    val urls_json = JArray(urls map { JString(_)})

    val p = Package("webapp", Set.empty, Map("healthcheck_paths" -> urls_json), "jetty-webapp", new File("/tmp/packages/webapp"))
    val jetty = new JettyWebappPackageType(p)
    val host = Host("host_name")

    jetty.actions("deploy")(host) should be (List(
      BlockFirewall(host as "jetty"),
      CopyFile(host as "jetty", "/tmp/packages/webapp/*", "/jetty-apps/webapp/"),
      Restart(host as "jetty", "webapp"),
      WaitForPort(host, "8080", 20 seconds),
      CheckUrls(host, "8080", urls, 20 seconds),
      UnblockFirewall(host as "jetty")
    ))

  }

  it should "allow servicename to be overridden for copy and restart" in {
    val p = Package("webapp", Set.empty, Map.empty, "jetty-webapp", new File("/tmp/packages/webapp"))
    val jetty = new JettyWebappPackageType(p)
    val p2 = Package("webapp", Set.empty, Map("servicename"->"microapps"), "jetty-webapp", new File("/tmp/packages/webapp"))
    val jetty2 = new JettyWebappPackageType(p2)

    val host = Host("host_name")

    jetty.actions("deploy")(host) should (contain[Task] (
      CopyFile(host as "jetty", "/tmp/packages/webapp/*", "/jetty-apps/webapp/")
    ) and contain[Task] (
      Restart(host as "jetty", "webapp")
    ))

    jetty2.actions("deploy")(host) should (contain[Task] (
      CopyFile(host as "jetty", "/tmp/packages/webapp/*", "/jetty-apps/microapps/")
    ) and contain[Task] (
      Restart(host as "jetty", "microapps")
    ))

  }

  "django web app package type" should "have a deploy action" in {
    val p = Package("webapp", Set.empty, Map.empty, "django-webapp", new File("/tmp/packages/webapp-build.7"))
    val django = new DjangoWebappPackageType(p)
    val host = Host("host_name")

    django.actions("deploy")(host) should be (List(
      BlockFirewall(host as "django"),
      SetSwitch(host, "80", "HEALTHCHECK_OK", false),
      CopyFile(host as "django", "/tmp/packages/webapp-build.7", "/django-apps/"),
      LinkFile(host as "django", "/django-apps/webapp-build.7", "/django-apps/webapp"),
      GracefulApache(host as "django"),
      WaitForPort(host, "80", 20 seconds),
      SetSwitch(host, "80", "HEALTHCHECK_OK", true),
      UnblockFirewall(host as "django")
    ))
  }
}