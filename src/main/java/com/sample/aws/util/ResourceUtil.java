package com.sample.aws.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceUtil {

	private static Logger log = LoggerFactory.getLogger(ResourceUtil.class);

	public static Properties getResource(String resource) {
		return getResource(resource, false);
	}

	public static synchronized Properties getResource(String resource, boolean useUserDirectory) {
		Properties prop = new Properties();
		InputStream in = null;
		if (!useUserDirectory) {
			in = ResourceUtil.class.getClassLoader().getResourceAsStream(resource + ".properties");
		} else {
			try {
				in = new FileInputStream(System.getProperty("user.dir") + File.separator + resource + ".properties");
			} catch (FileNotFoundException e) {
				log.error("Error updating the property file : ", e);
			}
		}
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

	public static synchronized void updateResource(String propertyFileName, KeyValue<String, String>[] properties) {
		FileOutputStream out;
		try {
			String filePath = System.getProperty("user.dir") + File.separator + propertyFileName + ".properties";
			FileInputStream in = new FileInputStream(filePath);
			Properties props = new Properties();
			props.load(in);
			in.close();

			out = new FileOutputStream(filePath);
			for (KeyValue<String, String> keyValue : properties) {
				props.setProperty(keyValue.getKey(), keyValue.getValue());
			}
			props.store(out, null);
			out.close();
			log.info("Resource {} file updated", filePath);
		} catch (FileNotFoundException e) {
			log.error("Error updating the property file : ", e);
		} catch (IOException ioe) {
			log.error("Error updating the property file : ", ioe);
		}
	}

}
