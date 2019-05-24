
package com.albb.springmvc.controller;

import com.albb.springmvc.annotation.Autowired;
import com.albb.springmvc.annotation.Controller;
import com.albb.springmvc.annotation.RequestMapping;
import com.albb.springmvc.annotation.RequestParam;
import com.albb.springmvc.service.SpringService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @Description: controller类
 * @className: SpringController
 * @author: wb-lxh438576
 * @date: 2019/3/13 下午 05:27
 */
@Controller
@RequestMapping("/springMVC")
public class SpringController {
    @Autowired("springService")
    private SpringService springService;

    @RequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @RequestParam("name") String name, @RequestParam("age") String age) {
        try {
            PrintWriter printWriter = response.getWriter();
            String result = springService.query(name, age);
            printWriter.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}