package com.sample.aws.sns.http.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClientBuilder;
import com.amazonaws.services.iot.model.Action;
import com.amazonaws.services.iot.model.CreateTopicRuleRequest;
import com.amazonaws.services.iot.model.SnsAction;
import com.amazonaws.services.iot.model.TopicRulePayload;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sample.aws.util.KeyValue;
import com.sample.aws.util.ResourceUtil;

public class IamServlet extends HttpServlet {

	private static Logger log = LoggerFactory.getLogger(ResourceUtil.class);
	private static Properties awsProperties = ResourceUtil.getResource("aws");
	private static Properties iamProperties = ResourceUtil.getResource("iam");

	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		log.info("Iam Request:{}:{}", req.getLocalAddr(), req.getLocalPort());
		resp.setContentType("application/json");
		resp.setStatus(HttpServletResponse.SC_OK);
		String userName = req.getParameter("username");
		if (userName == null || userName.isEmpty()) {
			try {

				resp.getWriter().println("{ \"Status\": \"Error\", \"Message\":\"User is null or empty\"}");
				return;

			} catch (IOException e) {
				log.error("Error occured while processing the response : ", e);
			}
		}

		// Create AWS user
		final AmazonIdentityManagement iam = AmazonIdentityManagementClientBuilder.defaultClient();

		CreateUserRequest request = new CreateUserRequest().withUserName(userName);
		CreateUserResult response = iam.createUser(request);

		log.info("Successfully created user: {}", response.getUser().getUserName());

		// Attche User Policies for SQS and SNS
		attachUserPolicy(iam, userName);
		//createIotRolePolicy(iam);
		
		// Recreate policy
		recreateSecurity(iam, userName);

		try {

			resp.getWriter().println("{ \"Status\": \"Success\", \"Message\":\"User " + response.getUser().getUserName()
					+ " created\"}");

		} catch (IOException e) {
			log.error("Error occured while processing the response : ", e);
		}
	}

	private void attachUserPolicy(AmazonIdentityManagement iam, String userName) {

		AttachUserPolicyRequest attach_sns_request = new AttachUserPolicyRequest().withUserName(userName)
				.withPolicyArn(iamProperties.getProperty("policy.arn.sns"));

		iam.attachUserPolicy(attach_sns_request);

		AttachUserPolicyRequest attach_sqs_request = new AttachUserPolicyRequest().withUserName(userName)
				.withPolicyArn(iamProperties.getProperty("policy.arn.sqs"));

		iam.attachUserPolicy(attach_sqs_request);

		log.info("Successfully Attached policies for the user {}", userName);
	}
	
//	private void createIotRolePolicy(AmazonIdentityManagement iam) {
//
//		AttachRolePolicyRequest attach_sns_request = new AttachRolePolicyRequest().withRoleName("")
//				.withPolicyArn(iamProperties.getProperty("policy.arn.sns"));
//
//		iam.attachRolePolicy(attach_sns_request);
//
//		AttachRolePolicyRequest attach_sqs_request = new AttachRolePolicyRequest().withRoleName(userName)
//				.withPolicyArn(iamProperties.getProperty("policy.arn.sqs"));
//
//		iam.attachRolePolicy(attach_sqs_request);
//
//		log.info("Successfully Attached Role policies for the user {}", userName);
//	}

	private void recreateSecurity(AmazonIdentityManagement iam, String userName) {

		boolean done = false;
		ListAccessKeysRequest request = new ListAccessKeysRequest().withUserName(userName);

		while (!done) {

			ListAccessKeysResult response = iam.listAccessKeys(request);

			for (AccessKeyMetadata metadata : response.getAccessKeyMetadata()) {
				log.info("Retrieved access key {}", metadata.getAccessKeyId());
				DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
						.withAccessKeyId(metadata.getAccessKeyId()).withUserName(userName);

				iam.deleteAccessKey(deleteAccessKeyRequest);
			}

			request.setMarker(response.getMarker());

			if (!response.getIsTruncated()) {
				done = true;
			}
		}

		CreateAccessKeyRequest keyRequest = new CreateAccessKeyRequest().withUserName(userName);
		CreateAccessKeyResult keyResult = iam.createAccessKey(keyRequest);

		log.info("Created access key: {}", keyResult.getAccessKey());

		// Update properties
		KeyValue accessKeys[] = { new KeyValue("username", keyResult.getAccessKey().getUserName()),
				new KeyValue<String, String>("access_key", keyResult.getAccessKey().getAccessKeyId()),
				new KeyValue<String, String>("seceret_key", keyResult.getAccessKey().getSecretAccessKey()) };

		ResourceUtil.updateResource("aws", accessKeys);

		configureIoTPlatform();
	}

	private void configureIoTPlatform() {

		// createDevice();

		createIotSnsRule("testthing");
	}

	private void createDevice(String deviceName) {

	}

	private void createIotSnsRule(String deviceName) {
		TopicRulePayload topicRulePayload = new TopicRulePayload();
		topicRulePayload.withSql("SELECT * FROM '$aws/things/" + deviceName + "/shadow/update/delta'");
		topicRulePayload.setRuleDisabled(false);

		SnsAction snsAction = new SnsAction();
		snsAction.setTargetArn("arn:aws:sns:*:*:my_sns_topic");
		snsAction.setRoleArn("arn:aws:iam::015167565503:role/Aepona-SunethDeSoyza-Developer");
		Action action = new Action();
		action.setSns(snsAction);
		List<Action> actions = new ArrayList<>();
		actions.add(action);
		topicRulePayload.setActions(actions);

		CreateTopicRuleRequest topicRuleRequest = new CreateTopicRuleRequest();
		topicRuleRequest.setTopicRulePayload(topicRulePayload);
		topicRuleRequest.setRuleName("testrule");
		AWSIot awsIot = AWSIotClientBuilder.defaultClient();
		awsIot.createTopicRule(topicRuleRequest);
		log.info("Rule created : {} ", topicRulePayload.toString());

	}

	private JsonObject convertAccessKey(String accessKey) {
		JsonObject retVal = new JsonObject();
		String allKeys[] = accessKey.split(",");
		for (String key : allKeys) {
			String keyValue[] = key.split(":");
			retVal.addProperty(keyValue[0], keyValue[1]);
		}

		return retVal;
	}
}
