import controllers.Logging
import lifecycle.Lifecycle
import org.reflections.Reflections
import play.mvc.Http.RequestHeader
import play.mvc.Result
import play.{Application, GlobalSettings}
import play.api.mvc.Results.InternalServerError
import scala.collection.JavaConversions._

class Global extends GlobalSettings with Logging {
  // find all objects that mixin Lifecycle and get their singletons
  log.info("Finding singletons requiring lifecycle management")
  val reflections = new Reflections("")
  val lifecycleClasses = reflections.getSubTypesOf(classOf[Lifecycle]).toList
  log.info("Found following singletons requiring lifecycle management: %s" format (lifecycleClasses.map{_.getName}.mkString))
  val lifecycleSingletons = lifecycleClasses.map{ _.getField("MODULE$").get(null).asInstanceOf[Lifecycle] }

  override def onStart(app: Application) {
    lifecycleSingletons foreach (_.init())
  }

  override def onStop(app: Application) {
    lifecycleSingletons foreach(_.shutdown())
  }

  override def onError(request: RequestHeader, t: Throwable) = {
    log.error("Error whilst trying to serve request", t)
    val reportException = if (t.getCause != null) t.getCause else t
    new Result() { def getWrappedResult = InternalServerError(views.html.errorPage(reportException)) }
  }
}
