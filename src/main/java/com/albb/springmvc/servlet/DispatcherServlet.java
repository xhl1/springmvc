package com.albb.springmvc.servlet;

import com.albb.springmvc.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {
    //全类名路径
    List<String> classNames = new ArrayList<>();
    //所有实例化的bean
    Map<String, Object> beansMap = new HashMap<>();
    //存放url和method之间的关系
    Map<String, Object> handlerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //IOC初始化
        //把所有的bean扫描----扫描所有的class文件
        scanPackage("com.albb");
        //根据扫描出的全类名进行实例化bean
        doInstance();
        //根据bean注入依赖关系
        doIoc();
        //建立url和method之间的关系
        buildUrlMapping();
    }


    /**
     * Description :扫描com.albb下的文件
     *
     * @param basePackage : 包名com.albb
     * @return : void
     * @author : wb-lxh
     * @date : 2019/3/26 9:49 PM
     */
    private void scanPackage(String basePackage) {
        //把"."换成"/"
        URL uri = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
        String fileStr = uri.getFile();
        File file = new File(fileStr);
        String[] filesStr = file.list();//这里拿到springmvc下的所有文件
        for (String path : filesStr) {
            File filePath = new File(fileStr + path);//这里是拿到com.albb.springmvc下的所有内容
            //判断是否是文件夹
            if (filePath.isDirectory()) {
                //递归
                scanPackage(basePackage + "." + path);
            } else {
                //如果是class文件就加入list中
                classNames.add(basePackage + "." + filePath.getName());//com....class
            }
        }
    }

    /**
     * Description :进行实例化
     *
     * @return : void
     * @author : wb-lxh
     * @date : 2019/3/26 9:58 PM
     */
    private void doInstance() {
        if (classNames.size() > 0) {
            //list的所有class类,对这些类进行实例化
            for (String className : classNames) {
                //去掉".class"后缀
                String replace = className.replace(".class", "");
                try {
                    //动态加载类
                    Class<?> clazz = Class.forName(replace);
                    //判断类上面的是否是Controller注解
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        //创建Controller类实例化对象
                        Object instace = clazz.newInstance();
                        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                        if (null != requestMapping) {
                            String value = requestMapping.value();//拿到"/springMVC"路径
                            beansMap.put(value, instace);
                        }
                    } else if (clazz.isAnnotationPresent(Service.class)) {
                        Object instace = clazz.newInstance();
                        Service service = clazz.getAnnotation(Service.class);
                        if (null != service) {
                            String value = service.value();
                            beansMap.put(value, instace);
                        }
                    } else {
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("包扫描失败!");
        }
    }


    /**
     * Description : 把service注入到controller
     *
     * @return : void
     * @author : wb-lxh
     * @date : 2019/3/26 10:02 PM
     */
    private void doIoc() {
        if (beansMap.entrySet().size() > 0) {
            //把map里所有实例化的类遍历出来
            for (Map.Entry<String, Object> entry : beansMap.entrySet()) {
                //拿到实例化的类
                Object instace = entry.getValue();
                Class<?> clazz = instace.getClass();
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.isAnnotationPresent(Autowired.class)) {
                            Autowired annotation = field.getAnnotation(Autowired.class);
                            String key = annotation.value();
                            //private 打开权限
                            field.setAccessible(true);
                            try {
                                field.set(instace, beansMap.get(key));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            continue;
                        }
                    }
                } else {
                    continue;
                }
            }
        } else {
            System.out.println("没有一个实例化的类");
        }
    }

    private void buildUrlMapping() {
        if (beansMap.entrySet().size() > 0) {
            for (Map.Entry<String, Object> entry : beansMap.entrySet()) {
                Object instace = entry.getValue();
                Class<?> clazz = instace.getClass();
                if (clazz.isAnnotationPresent(Controller.class)) {
                    RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                    String classPath = requestMapping.value();//拿到"/springMVC"
                    Method[] methods = clazz.getMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                            String methodPath = annotation.value();
                            handlerMap.put(classPath + methodPath, method);
                        } else {
                            continue;
                        }
                    }
                } else {
                    continue;
                }
            }
        } else {
            System.out.println("没有一个实例化的类");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取请求路径 /springmvc/springMVC/query
        String requestURI = req.getRequestURI();
        //获取到/springmvc   getContextPath()可返回站点的根路径,也就是项目的名字
        String contextPath = req.getContextPath();
        String path = requestURI.replace(contextPath, "");
        Method method = (Method) handlerMap.get(path);
        Object springController = beansMap.get("/" + path.split("/")[1]);
        Object arg[] = hand(req, resp, method);
        try {
            method.invoke(springController, arg);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Description : 非策略者模式
     *
     * @param request  :
     * @param response :
     * @param method   :
     * @return : java.lang.Object[]
     * @author : wb-lxh
     * @date : 2019/3/26 10:04 PM
     */
    private static Object[] hand(HttpServletRequest request, HttpServletResponse response, Method method) {
        //拿到当前待执行的方法有哪些参数
        Class<?>[] parameterClazzs = method.getParameterTypes();
        //根据参数的个数,new 一个参数的数组,将方法里的所有参数赋值到args来
        Object[] args = new Object[parameterClazzs.length];
        int args_i = 0;
        int index = 0;
        for (Class<?> parameterClazz : parameterClazzs) {
            if (ServletRequest.class.isAssignableFrom(parameterClazz)) {
                args[args_i++] = request;
            }
            if (ServletResponse.class.isAssignableFrom(parameterClazz)) {
                args[args_i++] = response;
            }
            //从0-3判断有没有RequestParam注解,很明显parameterType为0和1时,不是,
            //当为2和3时为@RequestParam,需要解析
            //[com.albb.springmvc.annotation.RequestParam(value=name)]
            Annotation[] parameterAns = method.getParameterAnnotations()[index];
            if (parameterAns.length > 0) {
                for (Annotation parameterAn : parameterAns) {
                    if (RequestParam.class.isAssignableFrom(parameterAn.getClass())) {
                        RequestParam requestParam = (RequestParam) parameterAn;
                        //找到注解里的name和age
                        args[args_i++] = request.getParameter(requestParam.value());
                    }
                }
            }
            index++;
        }
        return args;
    }

}
