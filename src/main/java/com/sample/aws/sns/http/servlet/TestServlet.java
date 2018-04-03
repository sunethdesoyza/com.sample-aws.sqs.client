package com.sample.aws.sns.http.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sample.aws.sns.http.HttpListener;

public class TestServlet extends HttpServlet {
	
	private static Logger log = LoggerFactory.getLogger(TestServlet.class);

	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("application/json");
		resp.setStatus(HttpServletResponse.SC_OK);
		try {
			resp.getWriter().println("{ \"status\": \"ok\"}");
		} catch (IOException e) {
			log.error("Error occured while processing the response : ",e);
		}
	}
}
