import scalaz.{ Node => ZNode, Source => ZSource, _ }
import Scalaz._

package object domc {
	type Safe[T]	= ValidationNel[String,T]
} 
