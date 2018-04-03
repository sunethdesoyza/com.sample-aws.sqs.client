package com.aepona.concert.md;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SqsSamplePublisher {

	public static void main(String[] args) {
		new Thread(() -> {
			while (true) {
				ResourceBundle properties = ResourceBundle.getBundle("aws");
                ResourceBundle sqs_properties = ResourceBundle.getBundle("sqs");

				BasicAWSCredentials awsCredentials = new BasicAWSCredentials(properties.getString("access_key"),
						properties.getString("seceret_key"));
				AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
				builder.setRegion("eu-west-1");
				AmazonSQSClient client = (AmazonSQSClient) builder
						.withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();

				String queue_url = client.getQueueUrl(sqs_properties.getString("topic")).getQueueUrl();

				SendMessageRequest send_msg_request = new SendMessageRequest().withQueueUrl(queue_url)
						.withMessageBody("hello world @ " + new Date().getTime()).withDelaySeconds(5);

				client.sendMessage(send_msg_request);
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();;
	}

}
