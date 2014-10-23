package xsbtDomc

import scala.collection.immutable.{ Seq => ISeq }

object Safe {
	def win[F,W](value:W):Safe[F,W]	=
			new Safe[F,W] {
				def cata[X](fail:Nes[F]=>X, win:W=>X):X	= win(value)
			}
			
	def fail[F,W](problems:Nes[F]):Safe[F,W]	=
			new Safe[F,W] {
				def cata[X](fail:Nes[F]=>X, win:W=>X):X	= fail(problems)
			}
		
	/*
	def catchException[W](block: =>W):Safe[Exception,W]	=
			try { win(block) }
			catch { case e:Exception => fail(e.nes) }
	*/
		
	def traverseISeq[F,S,T](func:S=>Safe[F,T]):ISeq[S]=>Safe[F,ISeq[T]]	= 
			ss	=> {
				(ss map func foldLeft win[F,ISeq[T]](Vector.empty[T])) { (old, cur) =>
					old zip cur cata (
						(zipFail:Nes[F])		=> fail(zipFail),
						{ case (oldWin, curWin)	=> win(oldWin :+ curWin) }
					)
				}
			}
			
	//------------------------------------------------------------------------------
	
	implicit class Nessify[T](peer:T) {
		def nes:Nes[T]	= Nes single peer
	}
	
	implicit class Optional[T](peer:Option[T]) {
		def toSafe[F](problems: =>Nes[F]):Safe[F,T]	=
				peer match {
					case Some(x)	=> win(x)
					case None		=> fail(problems)
				}
	}
	
	implicit class Conditional(peer:Boolean) {
		def safeGuard[F](problems: =>Nes[F]):Safe[F,Unit]	=
				if (peer)	win(())
				else		fail(problems)
			
		def safePrevent[F](problems: =>Nes[F]):Safe[F,Unit]	=
				if (peer)	fail(problems)
				else		win(())
	}
	
	implicit class Problematic[T](peer:ISeq[T]) {
		def preventing[W](value: =>W):Safe[T,W]	=
				Nes fromISeq peer map fail getOrElse win(value)
	}
}

sealed trait Safe[+F,+W] {
	def cata[X](fail:Nes[F]=>X, win:W=>X):X
	
	def isWin:Boolean	= cata(_ => false, _ => true)
	def isFail:Boolean	= !isWin
	
	def forEach(func:W=>Unit) {
		cata(_ => (), func)
	}
	
	def map[U](func:W=>U):Safe[F,U]	=
			cata(
				Safe.fail,
				func andThen Safe.win
			)
			
	def flatMap[FF>:F,U](func:W=>Safe[FF,U]):Safe[FF,U]	=
			cata(
				Safe.fail,
				func
			)
			
	def zip[FF>:F,U](that:Safe[FF,U]):Safe[FF,(W,U)]	=
			this cata (
				thisProblems	=> {
					that cata (
						thatProblems	=> Safe fail (thisProblems ++ thatProblems),
						thatResult		=> Safe fail (thisProblems)
					)
				},
				thisResult	=> {
					that cata (
						thatProblems	=> Safe fail (thatProblems),
						thatResult		=> Safe win ((thisResult, thatResult))
					)
				}
			)
			
	def toOption:Option[W]	=
			cata(
				_ => None,
				Some.apply
			)
}
