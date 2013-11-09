package domc

object Nel {
	def single[T](head:T):Nel[T]	=
			Nel(head, Seq.empty)
		
	def fromSeq[T](it:Seq[T]):Option[Nel[T]]	=
			if (it.nonEmpty)	Some(Nel(it.head, it.tail))
			else				None
}

case class Nel[+T](head:T, tail:Seq[T]) {
	def ++[U>:T](that:Nel[U]):Nel[U]	=
			Nel(this.head, (this.tail :+ that.head) ++ that.tail)
	
	def toSeq:Seq[T]	=
			head +: tail
}
