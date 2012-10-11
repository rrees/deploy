package datastore

import com.novus.salat.{TypeHintFrequency, StringTypeHintStrategy, Context}
import play.api.Play
import play.api.Play.current
import java.util.UUID
import deployment.DeployRecord
import lifecycle.Lifecycle
import com.novus.salat.dao.{SalatDAO, ModelCompanion}
import org.bson.types.ObjectId
import se.radley.plugin.salat._
import magenta.MessageStack
import com.novus.salat.StringTypeHintStrategy
import com.mongodb.casbah.commons.MongoDBObject

package object mongoContext {
  implicit val context = {
    val context = new Context {
      val name = "global"
      override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
    }
    context.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
    context.registerClassLoader(Play.classloader)
    context
  }
}

object MongoDatastore extends DataStore with Lifecycle {
  def init() { DataStore.register(this) }
  def shutdown() { DataStore.unregister(this) }

  def createDeploy(record: DeployRecord) { MongoDeployRecord.save(record) }
  def updateDeploy(uuid: UUID, stack: MessageStack) {
    MongoDeployRecord.findOneByUUID(uuid) foreach { record =>
      MongoDeployRecord.update()
    }
  }
  def getDeploy(uuid: UUID) = None
}


object MongoDeployRecord extends ModelCompanion[DeployRecord, ObjectId] {
  import mongoContext.context
  val dao = new SalatDAO[DeployRecord, ObjectId](collection = mongoCollection("deploys")) {}
  def findOneByUUID(uuid:UUID): Option[DeployRecord] = dao.findOne(MongoDBObject("uuid" -> uuid))
}