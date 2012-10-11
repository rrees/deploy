package datastore

import collection.mutable
import java.util.UUID
import deployment.DeployRecord
import magenta.MessageStack

trait DataStore {
  def createDeploy(record:DeployRecord)
  def updateDeploy(uuid:UUID, stack: MessageStack)
  def getDeploy(uuid:UUID):Option[DeployRecord]
}

object DataStore extends DataStore{
  val datastores = mutable.Buffer[DataStore]()

  def register(store: DataStore) { datastores += store }
  def unregister(store: DataStore) { datastores -= store }

  def createDeploy(record:DeployRecord) {
    datastores.foreach(_.createDeploy(record))
  }

  def updateDeploy(uuid: UUID, stack: MessageStack) {
    datastores.foreach(_.updateDeploy(uuid,stack))
  }

  def getDeploy(uuid: UUID) = None
}


