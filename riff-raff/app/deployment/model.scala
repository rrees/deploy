package deployment

import java.util.UUID
import magenta._
import java.io.File
import magenta.teamcity.Artifact.build2download
import magenta.DeployParameters
import magenta.ReportTree
import magenta.MessageStack

object Task extends Enumeration {
  val Deploy = Value("Deploy")
  val Preview = Value("Preview")
}

case class DeployRecord(taskType: Task.Value,
                        uuid: UUID,
                        parameters: DeployParameters,
                        messages: List[MessageStack] = Nil) {
  lazy val report:ReportTree = DeployReport(messages, "Deployment Report")
  lazy val buildName = parameters.build.projectName
  lazy val buildId = parameters.build.id
  lazy val deployerName = parameters.deployer.name
  lazy val stage = parameters.stage
  lazy val isRunning = report.isRunning

  def +(message: MessageStack): DeployRecord = {
    this.copy(messages = messages ++ List(message))
  }
  def loggingContext[T](block: => T): T = {
    MessageBroker.deployContext(uuid, parameters) { block }
  }
  def withDownload[T](block: File => T): T = {
    parameters.build.withDownload(block)
  }
}