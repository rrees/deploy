package magenta
package data

import org.specs2.mutable.Specification
import com.mongodb.casbah.Imports._
import org.specs2.execute.Success
import org.bson.types.ObjectId
import com.mongodb.WriteConcern
import util.Random
import com.mongodb.casbah.commons.MongoDBObject

import com.novus.salat._
import dao.SalatDAO
import model._



case class TestApp(name: String, id: ObjectId = ObjectId.get) {
  def save() { TestApp.save(this) }
}


object TestApp extends SalatDAO[TestApp, ObjectId](collection = MongoStorage.collection("test_app")) {
  collection.ensureIndex(Map("name" -> 1), "unique_name", true)
}


class HackingInSomeDataTest extends Specification {

  "TestApp" should {
    "be able to be stored in mongo" in {

      val newApp = TestApp("Some new app: " +  Random.nextString(10))
      newApp.save()

      val loadedApp = TestApp.findOneByID(newApp.id) getOrElse failure("not loaded")

      newApp must_== loadedApp
    }
  }
}