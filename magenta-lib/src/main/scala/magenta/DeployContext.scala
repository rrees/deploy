package magenta

import tasks.Task

object DeployContext {
  def apply(parameters: DeployParameters, project: Project, deployInfo: DeployInfo): DeployContext = {
    val tasks = {
      MessageBroker.info("Resolving tasks...")
      val taskList = Resolver.resolve(project, deployInfo, parameters)
      MessageBroker.taskList(taskList)
      taskList
    }
    DeployContext(parameters, project, tasks)
  }
}

case class DeployContext(parameters: DeployParameters, project: Project, tasks: List[Task]) {
  val deployer = parameters.deployer
  val buildName = parameters.build.projectName
  val buildId = parameters.build.id
  val recipe = parameters.recipe.name
  val stage = parameters.stage

  def execute(keyRing: KeyRing) {
    MessageBroker.deployContext(parameters) {
      if (tasks.isEmpty) MessageBroker.fail("No tasks were found to execute. Ensure the app(s) are in the list supported by this stage/host.")

      tasks.foreach { task =>
        MessageBroker.taskContext(task) {
          task.execute(keyRing)
        }
      }
    }
  }

}
