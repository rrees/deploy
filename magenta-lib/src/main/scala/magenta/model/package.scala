package magenta

import com.novus.salat.{TypeHintFrequency, StringTypeHintStrategy, Context}

package object model {
  implicit val ctx = new Context {
    val name = Some("Optional-Type-Hints")

    override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary)

    // now remap away to your heart's content
    registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
  }
}