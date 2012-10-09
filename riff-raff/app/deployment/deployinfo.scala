package deployment

import magenta.json.DeployInfoJsonReader
import magenta._
import akka.actor.ActorSystem
import akka.util.duration._
import controllers.Logging
import magenta.App
import conf.Configuration
import utils.ScheduledAgent

object DeployInfoManager extends Logging {
  private def getDeployInfo = {
    try {
      import sys.process._
      log.info("Populating deployinfo hosts...")
      val deployInfo = DeployInfoJsonReader.parse("/opt/bin/deployinfo.json".!!)
      log.info("Successfully retrieved deployinfo (%d hosts and %d data found)" format(deployInfo.hosts.size, deployInfo.data.values.map(_.size).reduce(_+_)))
      deployInfo
    } catch {
      case e => log.error("Couldn't gather deployment information", e)
      throw e
    }
  }

  val system = ActorSystem("deploy")
  val agent = ScheduledAgent[DeployInfo](1 minute, 1 minute)(getDeployInfo)

  def deployInfo = agent()

  def hostList = deployInfo.hosts
  def dataList = deployInfo.data

  def credentials(stage:String,apps:Set[App]) : List[Credentials] = {
    apps.toList.flatMap(app => deployInfo.firstMatchingData("aws-keys",app,stage)).map(k => Configuration.aws.credentials(k.value)).distinct
  }

  def keyRing(context:DeployContext): KeyRing = {
    KeyRing( SystemUser(keyFile = Some(Configuration.sshKey.file)),
                credentials(context.stage.name, context.project.applications))
  }

  def shutdown() {
    agent.shutdown()
  }
}