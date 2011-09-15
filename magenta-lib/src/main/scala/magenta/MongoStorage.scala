package magenta

import com.mongodb.WriteConcern
import com.mongodb.casbah.MongoConnection


object MongoStorage {
  private lazy val mongoConn = MongoConnection()

  private lazy val db = mongoConn("magenta")
  db.setWriteConcern(WriteConcern.SAFE)

  def collection(name: String) = db(name)
}
