package com.sunnyday.cqjz.utils;

import cn.hutool.core.io.resource.ClassPathResource;

import java.io.IOException;
import java.util.Properties;

/**
 * @author TMW
 * @since 2020/11/19 16:18
 */
public class PropertiesUtil {
    public static Properties get() {
        Properties properties = new Properties();
        try {
            ClassPathResource resource = new ClassPathResource("file.properties");
            properties.load(resource.getStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
