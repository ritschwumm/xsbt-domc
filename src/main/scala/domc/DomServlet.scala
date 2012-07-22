package domc

import java.io._

import javax.servlet.http._

import scutil.Implicits._
import scutil.Resource._
import scutil.Charsets._
import scutil.log._

import scwebapp._
import scwebapp.HttpImplicits._
import scwebapp.HttpInstances._
import scwebapp.HttpStatusEnum._
import scwebapp.StandardMimeTypes._

final class DomServlet extends HttpServlet with Logging {
	private val encoding				= utf_8
    private val text_javascript_charset	= text_javascript attribute ("charset", encoding.name)
	
    override def init() {}
	override def destroy() {}
    
    override def doGet(request:HttpServletRequest, response:HttpServletResponse) {
		handle(request, response)
	}
	
    override def doPost(request:HttpServletRequest, response:HttpServletResponse) {
		handle(request, response)
	}
	
	/*
	@volatile private var cache:Map[String,HttpResponder]	= Map.empty
	*/
	
	private def handle(request:HttpServletRequest, response:HttpServletResponse) {
		request		setEncoding	encoding
		response	setEncoding	encoding
		response.noCache()	// TODO no!
		
		def create:HttpResponder	= {
			val	path	= request.getServletPath
			if (path == null) {
				ERROR("resource not found for missing path")
				return SetStatus(NOT_FOUND)
			}
			
			if (path contains "..") {
				ERROR("resource not found for invalid path: " + path)
				return SetStatus(NOT_FOUND)
			}
			
			val url	= getServletConfig.getServletContext getResource path
			if (url == null) {
				ERROR("resource not found for path", path)
				return SetStatus(NOT_FOUND)
			}
			
			val text	= new InputStreamReader(url.openStream, encoding.name) use { _.readFully }
			DomTemplate compile text fold (
				err => {
					ERROR("cannot compile DOM for path", path)
					ERROR(err.list:_*)
					SetStatus(INTERNAL_SERVER_ERROR)
				},
				code => {
					// DEBUG("compiled DOM for path", path)
					SetStatus(OK)							~>
					SetContentType(text_javascript_charset)	~>
					SendString(code)
				}
			)
		}
		
		/*
		val cached:HttpResponder	= {
			val key	= request.getServletPath
			cache get key getOrElse {
				create doto { responder	=>
					cache	+= (key -> responder)
				}
			}
		}
		
		cached(response)
		*/
		
		create(response)
	}
}
