package domc

import scala.collection.immutable.{ Seq => ISeq }

object Nes {
	def single[T](head:T):Nes[T]	=
			Nes(head, ISeq.empty)
		
	def fromISeq[T](it:ISeq[T]):Option[Nes[T]]	=
			if (it.nonEmpty)	Some(Nes(it.head, it.tail))
			else				None
}

case class Nes[+T](head:T, tail:ISeq[T]) {
	def ++[U>:T](that:Nes[U]):Nes[U]	=
			Nes(this.head, (this.tail :+ that.head) ++ that.tail)
		
	def foreach(func:T=>Unit):Unit	=
			toISeq foreach func
	
	def toISeq:ISeq[T]	=
			head +: tail
}
