package teamcity

import java.net.URL
import conf.Configuration
import xml.{Node, Elem, XML}
import utils.ScheduledAgent
import akka.util.duration._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import controllers.Logging
import scala.Predef._
import collection.mutable
import magenta.DeployParameters

object `package` {
  type BuildTypeMap = Map[BuildType,List[Build]]

  implicit def buildTypeBuildsMap2latestBuildId(buildTypeMap: BuildTypeMap) = new {
    def latestBuildId(): Int = buildTypeMap.values.flatMap(_.map(_.buildId)).max
  }
}

trait BuildWatcher {
  def change(previous: BuildTypeMap, current: BuildTypeMap)
}

case class BuildType(id: String, name: String)

object Build {
  val dateTimeParser = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ")

  def apply(buildId: Int, name: String, number: String, startDate: String, branch: String): Build = {
    val parsedStartDate: DateTime = dateTimeParser.parseDateTime(startDate)
    apply(buildId: Int, name, number, parsedStartDate, branch)
  }

  def apply(build: Node): Build = {
    apply((build \ "@id" text).toInt, build \ "@buildTypeId" text, build \ "@number" text, build \ "@startDate" text, (build \ "@branchName").headOption.map(_.text).getOrElse("default"))
  }

  def apply(buildElements: Elem): List[Build] = {
    (buildElements \ "build").toList filter {
      build => (build \ "@status").text == "SUCCESS"
    } map { apply(_) }
  }
}
case class Build(buildId: Int, name: String, number: String, startDate: DateTime, branch: String)

object TeamCity extends Logging {
  private val listeners = mutable.Buffer[BuildWatcher]()
  def subscribe(sink: BuildWatcher) { listeners += sink }
  def unsubscribe(sink: BuildWatcher) { listeners -= sink }

  val tcURL = Configuration.teamcity.serverURL
  object api {
    val projectList = "/guestAuth/app/rest/projects"
    def buildList(buildTypeId: String) = "/guestAuth/app/rest/builds/?locator=buildType:%s,branch:branched:any" format buildTypeId
    def buildSince(buildId:Int) = "/guestAuth/app/rest/builds/?locator=sinceBuild:%d,branch:branched:any" format buildId
  }

  private val buildAgent = ScheduledAgent[BuildTypeMap](0 seconds, 1 minute, Map.empty[BuildType,List[Build]]){ currentBuildMap =>
    if (currentBuildMap.isEmpty || !getBuildsSince(currentBuildMap.latestBuildId()).isEmpty) {
      val buildTypes = getRetrieveBuildTypes
      val result = getSuccessfulBuildMap(buildTypes)
      log.info("Finished updating TC information (found %d buildTypes and %d successful builds)" format(result.size, result.values.map(_.size).reduce(_+_)))
      listeners.foreach{ listener =>
        try listener.change(currentBuildMap,result)
        catch {
          case e:Exception => log.error("Listener threw an exception", e)
        }
      }
      result
    } else currentBuildMap
  }

  def buildMap = buildAgent()
  def buildTypes = buildMap.keys.toList

  def successfulBuilds(projectName: String): List[Build] = buildTypes.filter(_.name == projectName).headOption
    .flatMap(buildMap.get(_)).getOrElse(Nil)

  def transformLastSuccessful(params: DeployParameters): DeployParameters = {
    if (params.build.id != "lastSuccessful")
      params
    else {
      val builds = successfulBuilds(params.build.projectName)
      builds.headOption.map{ latestBuild =>
        params.copy(build = params.build.copy(id = latestBuild.number))
      }.getOrElse(params)
    }
  }

  private def getRetrieveBuildTypes: List[BuildType] = {
    val url: URL = new URL(tcURL, api.projectList)
    log.info("Querying TC for build types: %s" format url)
    val projectElements = XML.load(url)
    (projectElements \ "project").toList.flatMap { project =>
      val btUrl: URL = new URL(tcURL, (project \ "@href").text)
      log.debug("Getting: %s" format btUrl)
      val buildTypeElements = XML.load(btUrl)
      (buildTypeElements \ "buildTypes" \ "buildType").map { buildType =>
        log.debug("found %s" format (buildType \ "@id"))
        BuildType(buildType \ "@id" text, "%s::%s" format(buildType \ "@projectName" text, buildType \ "@name" text))
      }
    }
  }

  private def getSuccessfulBuildMap(buildTypes: List[BuildType]): BuildTypeMap = {
    log.info("Querying TC for all successful builds")
    buildTypes.map(buildType => buildType -> getSuccessfulBuilds(buildType)).toMap
  }

  private def getSuccessfulBuilds(buildType: BuildType): List[Build] = {
    val url = new URL(tcURL, api.buildList(buildType.id))
    log.debug("Getting %s" format url.toString)
    val buildElements = XML.load(url)
    Build(buildElements)
  }

  private def getBuildsSince(buildId:Int): List[Build] = {
    log.info("Querying TC for all builds since %d" format buildId)
    val url = new URL(tcURL, api.buildSince(buildId))
    log.debug("Getting %s" format url.toString)
    val buildElements = XML.load(url)
    val builds = Build(buildElements)
    log.info("Found %d builds since %d" format (builds.size, buildId))
    log.info("Discovered builds: \n%s" format builds.mkString("\n"))
    builds
  }
}

