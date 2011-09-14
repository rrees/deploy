package magenta
package data

import org.specs2.mutable.Specification
import com.mongodb.casbah.Imports._
import org.specs2.execute.Success
import org.bson.types.ObjectId
import com.mongodb.WriteConcern
import util.Random
import com.mongodb.casbah.commons.MongoDBObject

object MongoStorage {
  private lazy val mongoConn = MongoConnection()
  private lazy val db = mongoConn("magenta")

  def collection(name: String) = db(name)
}

case class TestApp(name: String, _id: ObjectId = ObjectId.get) {
  def save() = TestApp.save(this)
}

object TestApp {
  private lazy val collection = MongoStorage.collection("test_app")

  collection.ensureIndex(Map("name" -> 1), "unique_name", true)
  collection.setWriteConcern(WriteConcern.SAFE)

  def save(a: TestApp) = collection += MongoDBObject("name" -> a.name, "_id" -> a._id)

  def loadById(id: ObjectId): Option[TestApp] = collection.findOneByID(id) flatMap conv


  private def conv(dbObject: DBObject): Option[TestApp] = for {
    name <- dbObject.getAs[String]("name")
    id <- dbObject.getAs[ObjectId]("_id")
  } yield TestApp(name, id)

}

class HackingInSomeDataTest extends Specification {

  "TestApp" should {
    "be able to be stored in mongo" in {

      val newApp = TestApp("Some new app: " +  Random.nextString(10))
      newApp.save()

      val loadedApp = TestApp.loadById(newApp._id) getOrElse failure("not loaded")

      newApp must_== loadedApp
    }
  }
}