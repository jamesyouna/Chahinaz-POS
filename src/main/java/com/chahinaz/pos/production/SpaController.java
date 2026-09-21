package com.chahinaz.pos.production;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.GetMapping;
@Controller public class SpaController{@GetMapping({"/sell","/sales","/registers","/reports","/inventory","/products","/admin"})public String spa(){return "forward:/index.html";}}
