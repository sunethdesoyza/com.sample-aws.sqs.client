package com.sample.aws.sqs;

import java.util.Date;
import java.util.Properties;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.sample.aws.util.ResourceUtil;

public class SqsSamplePublisher {
	
	private static Properties awsProperties = ResourceUtil.getResource("aws");
	private static Properties sqsProperties = ResourceUtil.getResource("sqs");

	public static void main(String[] args) {
		new Thread(() -> {
			while (true) {

				BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsProperties.getProperty("access_key"),
						awsProperties.getProperty("seceret_key"));
				AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
				builder.setRegion(awsProperties.getProperty("region"));
				AmazonSQSClient client = (AmazonSQSClient) builder
						.withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();

				String queue_url = client.getQueueUrl(sqsProperties.getProperty("topic")).getQueueUrl();

				SendMessageRequest send_msg_request = new SendMessageRequest().withQueueUrl(queue_url)
						.withMessageBody("Test SQS Message @ " + new Date().getTime()).withDelaySeconds(5);

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
