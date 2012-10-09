package conf

import play.api.Play
import play.api.mvc.{Action, Result, AnyContent, Request}
import com.gu.management._
import logback.LogbackLevelPage
import com.gu.management.play.{ Management => PlayManagement }
import com.gu.conf.ConfigurationFactory
import java.io.File
import magenta.S3Credentials
import java.net.URL


class Configuration(val application: String, val webappConfDirectory: String = "env") {
  protected val configuration = ConfigurationFactory.getConfiguration(application, webappConfDirectory)

  implicit def option2getOrException[T](option: Option[T]) = new {
    def getOrException(exceptionMessage: String): T = {
      option.getOrElse {
        throw new IllegalStateException(exceptionMessage)
      }
    }
  }

  object sshKey {
    lazy val path: String = configuration.getStringProperty("sshKey.path").getOrException("No private SSH key configured")
    lazy val file: File = new File(path)
  }

  object logging {
    lazy val verbose = configuration.getStringProperty("logging").map(_.equalsIgnoreCase("VERBOSE")).getOrElse(false)
  }

  object aws {
    def credentials(accessKey: String) = {
      val secretKey = configuration.getStringProperty("aws.secretAccessKey.%s" format accessKey).
        getOrException("No AWS secret access key configured for %s (should be configured as aws.secretAccessKey.<accessKey>" format accessKey)
      S3Credentials(accessKey,secretKey)
    }
    lazy val dynamoDbKey = configuration.getStringProperty("aws.dynamodb.accessKey").getOrException("No AWS key specified for DynamoDB to use")
    lazy val dynamoTablePrefix = configuration.getStringProperty("aws.dynamodb.tablePrefix").getOrElse("riffraff")
  }

  object irc {
    lazy val isConfigured = name.isDefined && host.isDefined && channel.isDefined
    lazy val name = configuration.getStringProperty("irc.name")
    lazy val host = configuration.getStringProperty("irc.host")
    lazy val channel = configuration.getStringProperty("irc.channel")
  }

  object teamcity {
    lazy val serverURL = new URL(configuration.getStringProperty("teamcity.serverURL").getOrException("Teamcity server URL not configured"))
  }

  object continuousDeployment {
    private lazy val ProjectToStageRe = """^(.+)->(.+)$""".r
    lazy val configLine = configuration.getStringProperty("continuous.deployment", "")
    lazy val buildToStageMap = configLine.split("\\s").flatMap{ entry =>
        entry match {
          case ProjectToStageRe(project, stageList) =>  Some(project -> stageList.split(",").toList)
          case _ => None
        }
    }.toMap
    lazy val enabled = configuration.getStringProperty("continuous.deployment.enabled", "false") == "true"
  }

  override def toString(): String = configuration.toString
}

object Configuration extends Configuration("riff-raff", webappConfDirectory = "env")

object Management extends PlayManagement {
  val applicationName = Play.current.configuration.getString("application.name").get

  val pages = List(
    new ManifestPage,
    new HealthcheckManagementPage,
    new Switchboard(applicationName, Switches.all),
    StatusPage(applicationName, Metrics.all),
    new LogbackLevelPage(applicationName)
  )
}

class TimingAction(group: String, name: String, title: String, description: String, master: Option[Metric] = None)
  extends TimingMetric(group, name, title, description, master) {

  def apply(f: Request[AnyContent] => Result): Action[AnyContent] = {
    Action {
      request =>
        measure {
          f(request)
        }
    }
  }
  def apply(f: => Result): Action[AnyContent] = {
    Action {
      measure {
        f
      }
    }
  }
}

object TimedAction extends TimingAction("webapp",
  "requests",
  "Requests",
  "Count and response time of requests")

object TimedCometAction extends TimingAction("webapp",
  "comet_requests",
  "Comet Requests",
  "Count and response time of comet requests")

object LoginCounter extends CountMetric("webapp",
  "login_attempts",
  "Login attempts",
  "Number of attempted logins")

object FailedLoginCounter extends CountMetric("webapp",
  "failed_logins",
  "Failed logins",
  "Number of failed logins")

object Metrics {
  val all: Seq[Metric] = Seq(TimedAction, TimedCometAction, LoginCounter, FailedLoginCounter)
}

object Switches {
  //  val switch = new DefaultSwitch("name", "Description Text")
  val all: Seq[Switchable] = List(Healthcheck.switch)
}

