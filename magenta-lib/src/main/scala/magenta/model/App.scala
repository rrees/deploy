package magenta.model

import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import magenta.MongoStorage
import com.mongodb.casbah.Imports._


case class App(name: String)

object TestApp extends SalatDAO[App, ObjectId](collection = MongoStorage.collection("app")) {
  collection.ensureIndex(Map("name" -> 1), "unique_name", true)
}
