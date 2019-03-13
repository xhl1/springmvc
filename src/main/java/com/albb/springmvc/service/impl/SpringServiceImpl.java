package com.albb.springmvc.service.impl;

import com.albb.springmvc.annotation.Service;
import com.albb.springmvc.service.SpringService;

@Service("springService")
public class SpringServiceImpl implements SpringService {
    @Override
    public String query(String name, String age) {
        return "name:" + name + ",age:" + age;
    }
}
