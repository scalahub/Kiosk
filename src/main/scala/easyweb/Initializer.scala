
/*

======== auto-generated code ========
WARNING !!! Do not edit! 
Otherwise your changes may be lost the next time this file is generated.
  
Instead edit the file that generated this code. Refer below for details

Class that generated this code: org.sh.easyweb.AutoWeb

Stacktrace of the call is given below:

org.sh.reflect.CodeGenUtil$:CodeGenUtil.scala:69
org.sh.easyweb.AutoWeb:AutoWeb.scala:62
org.sh.easyweb.AutoWeb:AutoWeb.scala:144
kiosk.KioskWeb$:KioskWeb.scala:29

*/
package easyweb {
  import javax.servlet.http.HttpServlet
  import javax.servlet.http.{HttpServletRequest => HReq}
  import javax.servlet.http.{HttpServletResponse => HResp}
  import org.sh.reflect.{DefaultTypeHandler, EasyProxy}


  class Initializer extends HttpServlet {

    val anyRefs = List(
      kiosk.Env,kiosk.Script,kiosk.ECC,kiosk.Box,kiosk.Reader
    )
    anyRefs.foreach(EasyProxy.addProcessor("", _, DefaultTypeHandler, true))
    def getReq(hReq:HReq) = {}
    override def doGet(hReq:HReq, hResp:HResp) = doPost(hReq, hResp)
    override def doPost(hReq:HReq, hResp:HResp) = {}
  }
}
