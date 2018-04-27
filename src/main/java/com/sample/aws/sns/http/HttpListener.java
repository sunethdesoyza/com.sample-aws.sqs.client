package com.sample.aws.sns.http;

import java.net.UnknownHostException;
import java.util.Properties;

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
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.sample.aws.sns.http.servlet.AmazonSNSServlet;
import com.sample.aws.sns.http.servlet.IamServlet;
import com.sample.aws.sns.http.servlet.TestServlet;
import com.sample.aws.util.ResourceUtil;

public class HttpListener {

	private static Logger log = LoggerFactory.getLogger(HttpListener.class);

	private static Properties listenerProperties = ResourceUtil.getResource("listener");
	private static Properties awsProperties = ResourceUtil.getResource("aws",false);
	private static Properties snsProperties = ResourceUtil.getResource("sns");
	private static Properties aimProperties = ResourceUtil.getResource("iam");

	private Server server;

	public static void main(String[] args) throws UnknownHostException {
		System.setProperty("aws.accessKeyId",aimProperties.getProperty("access_key"));
		System.setProperty("aws.secretKey",aimProperties.getProperty("seceret_key"));
		System.setProperty("aws.region",aimProperties.getProperty("region"));
		
		HttpListener httpListener = new HttpListener();

		httpListener.startListener();
		//httpListener.subscribeToAwsSns();
	}

	private void subscribeToAwsSns() throws UnknownHostException {
		log.info("Attempting to subscribe to the SNS topic @ {}" + snsProperties.getProperty("arn"));
		
		BasicAWSCredentials creds = new BasicAWSCredentials(awsProperties.getProperty("access_key"),
				awsProperties.getProperty("seceret_key"));
		AmazonSNSClientBuilder builder = AmazonSNSClientBuilder.standard();
		builder.setRegion(awsProperties.getProperty("region"));
		AmazonSNSClient snsClient = (AmazonSNSClient) builder.withCredentials(new AWSStaticCredentialsProvider(creds))
				.build();

		String address = snsProperties.getProperty("public.subscription.ip");
		String subscriptionUrl = snsProperties.getProperty("public.subscription.protocol") + "://" + address + ":"
				+ snsProperties.getProperty("public.subscription.port") + "/"
				+ snsProperties.getProperty("public.subscription.context.url");
		SubscribeRequest subscribeReq = new SubscribeRequest().withTopicArn(snsProperties.getProperty("arn"))
				.withProtocol(snsProperties.getProperty("public.subscription.protocol")).withEndpoint(subscriptionUrl);
		snsClient.subscribe(subscribeReq);

		log.info("Subscribed URL - " + subscriptionUrl);
		log.info("SubscribeRequest - " + snsClient.getCachedResponseMetadata(subscribeReq));
	}

	private void startListener() {
		server = new Server();
		ServerConnector connector = new ServerConnector(server);

		ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(TestServlet.class, "/"+listenerProperties.getProperty("test.context.url"));
		servletHandler.addServletWithMapping(AmazonSNSServlet.class, "/"+snsProperties.getProperty("public.subscription.context.url"));
		servletHandler.addServletWithMapping(IamServlet.class, "/user");
		
		connector.setPort(Integer.parseInt(listenerProperties.getProperty("port")));
		server.setConnectors(new Connector[] { connector });
		server.setHandler(servletHandler);
		
		try {
			server.start();
			log.info("Listener started at port : {}", listenerProperties.getProperty("port"));
		} catch (Exception e) {
			log.error("Error starting the listener : ", e);
		}

	}
}
