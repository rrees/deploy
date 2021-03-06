package magenta
package tasks

import scala.io.Source
import java.net.Socket
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead
import scala._
import java.io.{IOException, FileNotFoundException, File}
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import java.net.URL

object CommandLocator {
  var rootPath = "/opt/deploy/bin"
  def conditional(binary: String) = List("if", "[", "-f", rootPath+"/"+binary, "];", "then", rootPath+"/"+binary,";", "fi" )
}

case class CopyFile(host: Host, source: String, dest: String) extends ShellTask {
  override val taskHost = Some(host)
  val noHostKeyChecking = "-o" :: "UserKnownHostsFile=/dev/null" :: "-o" :: "StrictHostKeyChecking=no" :: Nil

  def commandLine = List("rsync", "-rv", source, "%s:%s" format(host.connectStr, dest))
  def commandLine(keyRing: KeyRing): CommandLine = {
    val keyFileArgs = keyRing.sshCredentials.keyFile.toList.flatMap("-i" :: _.getPath :: Nil)
    val shellCommand = CommandLine("ssh" :: noHostKeyChecking ::: keyFileArgs ::: Nil).quoted
    CommandLine(commandLine.commandLine.head :: "-e" :: shellCommand :: commandLine.commandLine.tail)
  }

  lazy val description = "%s -> %s:%s" format (source, host.connectStr, dest)

  override def execute(keyRing: KeyRing) {
    commandLine(keyRing).run()
  }
}

case class S3Upload(stage: Stage, bucket: String, file: File, cacheControlHeader: Option[String] = None) extends Task with S3 {
  private val base = file.getParent + "/"

  private val describe = "Upload %s %s to S3" format ( if (file.isDirectory) "directory" else "file", file )
  def description = describe
  def verbose = describe

  def execute(keyRing: KeyRing)  {
    val client = s3client(keyRing)
    val filesToCopy = resolveFiles(file)

    val requests = filesToCopy map { file =>
      putObjectRequestWithPublicRead(bucket, toKey(file), file, cacheControlHeader)
    }

    requests.par foreach { client.putObject }
  }

  def toKey(file: File) = stage.name + "/" + file.getAbsolutePath.replace(base, "")

  private def resolveFiles(file: File): Seq[File] =
    Option(file.listFiles).map { _.toSeq.flatMap(resolveFiles) } getOrElse (Seq(file)).distinct
}

case class BlockFirewall(host: Host) extends RemoteShellTask {
  def commandLine = CommandLocator conditional "block-load-balancer"
}

case class Restart(host: Host, appName: String) extends RemoteShellTask {
  def commandLine = List("sudo", "/sbin/service", appName, "restart")
}

case class UnblockFirewall(host: Host) extends RemoteShellTask {
  def commandLine =  CommandLocator conditional "unblock-load-balancer"
}

case class WaitForPort(host: Host, port: String, duration: Long) extends Task with RepeatedPollingCheck {
  override def taskHost = Some(host)
  def description = "to %s on %s" format(host.name, port)
  def verbose = "Wail until a socket connection can be made to %s:%s" format(host.name, port)

  def execute(keyRing: KeyRing) {
    check {
      try {
        new Socket(host.name, port.toInt).close()
        true
      } catch {
        case e: IOException => false
      }
    }
  }
}

case class CheckUrls(host: Host, port: String, paths: List[String], duration: Long) extends Task with RepeatedPollingCheck {
  override def taskHost = Some(host)
  def description = "check [%s] on %s" format(paths, host)
  def verbose = "Check that [%s] returns a 200" format(paths)

  def execute(keyRing: KeyRing) {
    for (path <- paths) check {
      val url = new URL( "http://%s:%s%s" format (host.connectStr, port, path) )
      try {
        val connection = url.openConnection()
        connection.setConnectTimeout( 2000 )
        connection.setReadTimeout( 5000 )
        Source.fromInputStream( connection.getInputStream )
        true
      } catch {
        // Note that MessageBroker.fail will always throw a runtime exception, so we
        // won't ever need to return false from this branch, but it's necessary for
        // type checks to pass
        case e: FileNotFoundException => MessageBroker.fail("404 Not Found", e); false
        case e => false
      }
    }
  }
}

trait RepeatedPollingCheck {
  def duration: Long

  def check(theCheck: => Boolean) {
    val expiry = System.currentTimeMillis() + duration

    def checkAttempt(currentAttempt: Int) {
      if (!theCheck) {
        if (System.currentTimeMillis() < expiry) {
          MessageBroker.verbose("Check failed on attempt #"+currentAttempt +"- Retrying")
          val sleepyTime = math.min(math.pow(2,currentAttempt).toLong*100, 10000)
          Thread.sleep(sleepyTime)
          checkAttempt(currentAttempt + 1)
        } else {
          MessageBroker.fail("Check failed to pass within %d milliseconds (tried %d times) - aborting" format (duration,currentAttempt))
        }
      }
    }
    checkAttempt(1)
  }
}


case class SayHello(host: Host) extends Task {
  override def taskHost = Some(host)
  def execute(keyRing: KeyRing) {
    MessageBroker.info("Hello to " + host.name + "!")
  }

  def description = "to " + host.name
  def verbose = fullDescription
}

case class EchoHello(host: Host) extends ShellTask {
  override def taskHost = Some(host)
  def commandLine = List("echo", "hello to " + host.name)
  def description = "to " + host.name
}

case class Link(host: Host, target: String, linkName: String) extends RemoteShellTask {
  def commandLine = List("ln", "-sfn", target, linkName)
}

case class ApacheGracefulStop(host: Host) extends RemoteShellTask {
  def commandLine = List("sudo", "/usr/sbin/apachectl", "graceful-stop")
}

case class ApacheStart(host: Host) extends RemoteShellTask {
  def commandLine = List("sudo", "/usr/sbin/apachectl", "start")
}

case class Mkdir(host: Host, path: String) extends RemoteShellTask {
	def commandLine = List("/bin/mkdir", "-p", path)
}


