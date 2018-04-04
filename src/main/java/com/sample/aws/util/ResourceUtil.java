package com.sample.aws.util;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtil {

	private static Logger log = LoggerFactory.getLogger(ResourceUtil.class);

	public static synchronized Properties getResource(String resource) {
		Properties prop = new Properties();
		InputStream in = ResourceUtil.class.getClassLoader().getResourceAsStream(resource + ".properties");
		if (in == null) {
			log.error("Sorry, unable to find {}", resource);
			return prop;
		}
		try {
			prop.load(in);
			in.close();
			
		} catch (IOException e) {

			log.error("Error loading the property file : ", e);
		}
		return prop;
	}

}
