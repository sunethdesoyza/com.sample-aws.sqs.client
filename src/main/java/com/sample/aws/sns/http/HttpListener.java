package com.sample.aws.sns.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.sample.aws.sns.http.servlet.AmazonSNSServlet;
import com.sample.aws.sns.http.servlet.TestServlet;

public class HttpListener {

	private static Logger log = LoggerFactory.getLogger(HttpListener.class);

	private static ResourceBundle listenerProperties = ResourceBundle.getBundle("listener");
	private static ResourceBundle awsProperties = ResourceBundle.getBundle("aws");
	private static ResourceBundle snsProperties = ResourceBundle.getBundle("sns");
	
	private Server server;
	
	public static void main(String[] args) throws UnknownHostException {
		HttpListener httpListener = new HttpListener();
		httpListener.subscribeToAwsSns();
		httpListener.startListener();
	}
	
	private void subscribeToAwsSns() throws UnknownHostException {
		
		BasicAWSCredentials creds = new BasicAWSCredentials(awsProperties.getString("access_key"), awsProperties.getString("seceret_key"));
		AmazonSNSClientBuilder builder = AmazonSNSClientBuilder.standard();
		AmazonSNSClient snsClient = (AmazonSNSClient) builder
		.withCredentials(new AWSStaticCredentialsProvider(creds)).build();
				
		String address = InetAddress.getLocalHost().getHostAddress();
		SubscribeRequest subscribeReq = new SubscribeRequest()
		   .withTopicArn(snsProperties.getString("arn"))
		   .withProtocol("http")
		   .withEndpoint("http://" + address + ":" + snsProperties.getString("port"));
		snsClient.subscribe(subscribeReq);
		
	}
	
	private void startListener() {
		server = new Server();
        ServerConnector connector = new ServerConnector(server);
        
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(TestServlet.class,"/test");
        servletHandler.addServletWithMapping(AmazonSNSServlet.class,"/sns");
        
        connector.setPort(Integer.parseInt(listenerProperties.getString("port")));
        server.setConnectors(new Connector[] {connector});
        server.setHandler(servletHandler);
        try {
			server.start();
			log.info("Listener started at port : {}",listenerProperties.getString("port"));
		} catch (Exception e) {
			log.error("Error starting the listener : ", e);
		}
        
	}
}
