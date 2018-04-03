package com.sample.aws.sqs;

import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

public class SQSSubscriberMS {
	private static Logger log = LoggerFactory.getLogger(SQSSubscriberMS.class);

	public static void main(String[] args) {
		ResourceBundle aws_properties = ResourceBundle.getBundle("aws");
		ResourceBundle sqs_properties = ResourceBundle.getBundle("sqs");

		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(aws_properties.getString("access_key"),
				aws_properties.getString("seceret_key"));

		
		AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
		builder.setRegion(aws_properties.getString("region"));
		AmazonSQSClient client = (AmazonSQSClient) builder
				.withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();

		String queue_url = client.getQueueUrl(sqs_properties.getString("topic")).getQueueUrl();
		Executor executor = Executors.newFixedThreadPool(5);
		boolean consumer_flag=true;
		
		while(consumer_flag) {
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queue_url).withWaitTimeSeconds(20);
			ReceiveMessageResult result = client.receiveMessage(receiveMessageRequest);
		
			executor.execute(new Processor(result, client, queue_url));
			
		}
		
	}
}

class Processor implements Runnable{
	
	private static Logger log = LoggerFactory.getLogger(Processor.class);
	private ReceiveMessageResult receiveMessageResult;
	private AmazonSQSClient amazonSQSClient;
	private String queueUrl;

	public Processor(ReceiveMessageResult receiveMessageResult, AmazonSQSClient amazonSQSClient, String queueUrl) {
		this.receiveMessageResult = receiveMessageResult;
		this.amazonSQSClient = amazonSQSClient;
		this.queueUrl=queueUrl;
	}
	@Override
	public void run() {
		
		//Processing Messages
		log.info("Printing Relust List {}", receiveMessageResult);
		
		//Delete Messages, after Processing 
		receiveMessageResult.getMessages().forEach(message -> {
			DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest();
			deleteMessageRequest.setQueueUrl(queueUrl);
			deleteMessageRequest.setReceiptHandle(message.getReceiptHandle());
			amazonSQSClient.deleteMessage(deleteMessageRequest);
		});
		
		
	}
	
}
