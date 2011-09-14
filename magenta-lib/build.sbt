resolvers += "beaucatcher" at "http://beaucatcher.org/repository/"

libraryDependencies ++= Seq(
	"net.databinder" %% "dispatch-http" % "0.8.5",
	"net.liftweb" %% "lift-json" % liftVersion,
	"net.liftweb" %% "lift-util" % liftVersion,
    "com.mongodb.casbah" % "casbah-core_2.9.0-1" % "2.1.5-1",
    "org.specs2" %% "specs2" % "1.6.1",
	"org.scalatest" %% "scalatest" % "1.6.1" % "test"
)
