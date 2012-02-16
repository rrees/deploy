package magenta

import util.DynamicVariable
import org.slf4j.LoggerFactory


trait Output {
  def verbose(s: => String)
  def info(s: => String)
  def warn(s: => String)
  def error(s: => String)
  def except(e: Throwable)
  def context[T](s: => String)(block: => T): T
}

trait IndentingContext { this: Output =>

  val currentIndent = new DynamicVariable[String]("")

  def context[T](s: => String)(block: => T) = {
    info(s)
    currentIndent.withValue(currentIndent.value + "  ") {
      block
    }
  }

  def indent(s: String) = currentIndent.value + s

}

object BasicConsoleOutput extends Output with IndentingContext {
  def verbose(s: => String) { }
  def info(s: => String) { Console.out.println(indent(s)) }
  def warn(s: => String) { Console.out.println(indent(s)) }
  def error(s: => String) { Console.err.println(indent(s)) }

  def except(e: Throwable) {}
}

object Slf4jOutput extends Output with IndentingContext {
  val logger = LoggerFactory.getLogger("magenta")

  def verbose(s: => String) { logger.debug(indent(s)) }
  def info(s: => String) { logger.info(indent(s)) }
  def warn(s: => String) { logger.warn(indent(s)) }
  def error(s: => String) { logger.error(indent(s)) }
  def except(e: Throwable) { logger.debug(indent("Exception"),e) }
}

object Log extends Output {
  val current = new DynamicVariable[Output](Slf4jOutput)

  def verbose(s: => String) { current.value.verbose(s) }
  def info(s: => String) { current.value.info(s) }
  def warn(s: => String) { current.value.warn(s) }
  def error(s: => String) { current.value.error(s) }
  def except(e: Throwable) { current.value.except(e) }

  def context[T](s: => String)(block: => T) = { current.value.context(s)(block) }
}
