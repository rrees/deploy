package com.gu.deploy
package tasks

import java.net.Socket
import java.io.IOException

case class CopyFile(host: Host, source: String, dest: String) extends ShellTask {
  def commandLine = List("scp", "-r", source, "%s:%s" format(host.connectStr, dest))
  lazy val description = "%s -> %s:%s" format (source, host.connectStr, dest)
}

case class BlockFirewall(host: Host) extends RemoteShellTask {
  def commandLine = "deploy-block-fw.sh"
}

case class Restart(host: Host, appName: String) extends RemoteShellTask {
  def commandLine = List("sudo", "/sbin/service", appName, "restart")
}

case class UnblockFirewall(host: Host) extends RemoteShellTask {
  def commandLine = "deploy-unblock-fw.sh"
}

case class WaitForPort(host: Host, port: String, duration: Long) extends Task {
  def description = "to %s on %s" format(host.name, port)
  def verbose = fullDescription
  val MAX_CONNECTION_ATTEMPTS: Int = 10


  private def checkSocketOpen(ignored:Int) = {
    try {
        new Socket(host.name, port.toInt).close()
        Some(true)
      }
    catch {
      case e:IOException => {
        Thread.sleep(duration/MAX_CONNECTION_ATTEMPTS)
        None
      }
    }

  }

  def execute() {
    val range: Seq[Int] = 1 to MAX_CONNECTION_ATTEMPTS
    Stream(range: _*).flatMap(checkSocketOpen).headOption match {
      case None => sys.error("Timed out")
      case _ =>
    }
  }
}


case class SayHello(host: Host) extends Task {
  def execute() {
    Log.info("Hello to " + host.name + "!")
  }

  def description = "to " + host.name
  def verbose = fullDescription
}

case class EchoHello(host: Host) extends ShellTask {
  def commandLine = List("echo", "hello to " + host.name)
  def description = "to " + host.name
}