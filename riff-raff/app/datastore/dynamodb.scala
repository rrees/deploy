package datastore

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient
import conf.Configuration
import org.joda.time.DateTime
import java.util.UUID
import com.recursivity.dynamo.DynamoTransformer._
import com.amazonaws.services.dynamodb.model.PutItemRequest


case class Deploy(
  uuid:String,
  date:DateTime,
  log:List[String]
                   )

object DynamoDb {
  val accessKey = Configuration.aws.dynamoDbKey
  val credentials = Configuration.aws.credentials(accessKey)
  val awsCredentials = new BasicAWSCredentials(credentials.accessKey, credentials.secretAccessKey)

  val client = new AmazonDynamoDBClient(awsCredentials)
  client.setEndpoint("https://dynamodb.eu-west-1.amazonaws.com")

  val tablePrefix = Configuration.aws.dynamoTablePrefix

  def putTestItem {
    val test=Deploy(UUID.randomUUID.toString, new DateTime(), List("log entry "+UUID.randomUUID.toString, "log entry "+UUID.randomUUID.toString))
    val testMap=toDynamo(test)
    val pir=new PutItemRequest().withTableName(tablePrefix+"-deploy").withItem(testMap)
    client.putItem(pir)
  }
}

