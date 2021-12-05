package http2.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

@Controller
public class HtmlController {
    @RequestMapping("/")
    @ResponseBody
    public String welcome() {
        return "index";
    }


    @RequestMapping(value = "/index", method = RequestMethod.GET)
    public String createLoginForm(HttpServletResponse response){
        response.setHeader("Content-Type","text/html");
        return "index";
    }

    @RequestMapping(value = "/index1", method = RequestMethod.GET)
    public String createLoginForm1(HttpServletResponse response){
        response.setHeader("Content-Type","text/html");
        return "index1";
    }
}
