
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
