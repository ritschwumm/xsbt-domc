package domc

object Nes {
	def single[T](head:T):Nes[T]	=
			Nes(head, Seq.empty)
		
	def fromSeq[T](it:Seq[T]):Option[Nes[T]]	=
			if (it.nonEmpty)	Some(Nes(it.head, it.tail))
			else				None
}

case class Nes[+T](head:T, tail:Seq[T]) {
	def ++[U>:T](that:Nes[U]):Nes[U]	=
			Nes(this.head, (this.tail :+ that.head) ++ that.tail)
	
	def toSeq:Seq[T]	=
			head +: tail
}
