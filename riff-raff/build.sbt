resolvers += "Guardian Github Snapshots" at "http://guardian.github.com/maven/repo-releases"

resolvers += Resolver.url("Typesafe Ivy Releases", url("http://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  "com.gu" %% "management-play" % "5.15",
  "com.gu" %% "management-logback" % "5.15",
  "com.gu" %% "configuration" % "3.6",
  "se.radley" %% "play-plugins-salat" % "1.1",
  "org.pircbotx" % "pircbotx" % "1.7",
  "com.typesafe.akka" % "akka-agent" % "2.0.2",
  "org.clapper" %% "markwrap" % "0.5.4"
)

ivyXML :=
  <dependencies>
    <exclude org="commons-logging"><!-- Conflicts with jcl-over-slf4j in Play. --></exclude>
    <exclude org="org.springframework"><!-- Because I don't like it. --></exclude>
  </dependencies>