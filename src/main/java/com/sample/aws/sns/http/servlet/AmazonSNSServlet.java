package com.sample.aws.sns.http.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sample.aws.sns.util.Message;
import com.sample.aws.sns.util.MessageReadUtil;

public class AmazonSNSServlet extends HttpServlet {
	
	private static Logger log = LoggerFactory.getLogger(AmazonSNSServlet.class);

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SecurityException {
		log.info("Request:{}:{}",request.getLocalAddr(),request.getLocalPort());
	    //Get the message type header.
	    String messagetype = request.getHeader("x-amz-sns-message-type");
	    //If message doesn't have the message type header, don't process it.
	    if (messagetype == null) {
	        return;
	    }

	    // Parse the JSON message in the message body
	    // and hydrate a Message object with its contents
	    // so that we have easy access to the name/value pairs
	    // from the JSON message.
	    Scanner scan = new Scanner(request.getInputStream());
	    StringBuilder builder = new StringBuilder();
	    while (scan.hasNextLine()) {
	        builder.append(scan.nextLine());
	    }
	    new MessageReadUtil();
		Message msg = MessageReadUtil.readMessageFromJson(builder.toString());

	    // The signature is based on SignatureVersion 1.
	    // If the sig version is something other than 1,
	    // throw an exception.
	    if (msg.getSignatureVersion().equals("1")) {
	        // Check the signature and throw an exception if the signature verification fails.
	        if (isMessageSignatureValid(msg)) {
	            log.info(">>Signature verification succeeded");
	        } else {
	            log.info(">>Signature verification failed");
	            throw new SecurityException("Signature verification failed.");
	        }    
	    } else {
	        log.info(">>Unexpected signature version. Unable to verify signature.");
	        throw new SecurityException("Unexpected signature version. Unable to verify signature.");
	    }

	    // Process the message based on type.
	    if (messagetype.equals("Notification")) {
	        //TODO: Do something with the Message and Subject.
	        //Just log the subject (if it exists) and the message.
	        String logMsgAndSubject = ">>Notification received from topic " + msg.getTopicArn();
	        if (msg.getSubject() != null) {
	            logMsgAndSubject += " Subject: " + msg.getSubject();
	        }
	        logMsgAndSubject += " Message: " + msg.getMessage();
	        log.info(logMsgAndSubject);
	    } else if (messagetype.equals("SubscriptionConfirmation")) {
	        //TODO: You should make sure that this subscription is from the topic you expect. Compare topicARN to your list of topics
	        //that you want to enable to add this endpoint as a subscription.

	        //Confirm the subscription by going to the subscribeURL location
	        //and capture the return value (XML message body as a string)
	        Scanner sc = new Scanner(new URL(msg.getSubscribeURL()).openStream());
	        StringBuilder sb = new StringBuilder();
	        while (sc.hasNextLine()) {
	            sb.append(sc.nextLine());
	        }
	        log.info(">>Subscription confirmation (" + msg.getSubscribeURL() + ") Return value: " + sb.toString());
	        //TODO: Process the return value to ensure the endpoint is subscribed.
	    } else if (messagetype.equals("UnsubscribeConfirmation")) {
	        //TODO: Handle UnsubscribeConfirmation message.
	        //For example, take action if unsubscribing should not have occurred.
	        //You can read the SubscribeURL from this message and
	        //re-subscribe the endpoint.
	        log.info(">>Unsubscribe confirmation: " + msg.getMessage());
	    } else {
	        //TODO: Handle unknown message type.
	        log.info(">>Unknown message type.");
	    }
	    log.info(">>Done processing message: " + msg.getMessageId());
	}
	
	private static boolean isMessageSignatureValid(Message msg) {
	    try {
	        URL url = new URL(msg.getSigningCertURL());
	        InputStream inStream = url.openStream();
	        CertificateFactory cf = CertificateFactory.getInstance("X.509");
	        X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
	        inStream.close();

	        Signature sig = Signature.getInstance("SHA1withRSA");
	        sig.initVerify(cert.getPublicKey());
	        sig.update(getMessageBytesToSign(msg));
	        return sig.verify(Base64.decodeBase64(msg.getSignature()));
	    }
	    catch (Exception e) {
	        throw new SecurityException("Verify method failed.", e);
	    }
	}
	
	private static byte [] getMessageBytesToSign (Message msg) {
	    byte [] bytesToSign = null;
	    if (msg.getType().equals("Notification"))
	        bytesToSign = buildNotificationStringToSign(msg).getBytes();
	    else if (msg.getType().equals("SubscriptionConfirmation") || msg.getType().equals("UnsubscribeConfirmation"))
	        bytesToSign = buildSubscriptionStringToSign(msg).getBytes();
	    return bytesToSign;
	}

	//Build the string to sign for Notification messages.
	public static String buildNotificationStringToSign(Message msg) {
	    String stringToSign = null;

	    //Build the string to sign from the values in the message.
	    //Name and values separated by newline characters
	    //The name value pairs are sorted by name 
	    //in byte sort order.
	    stringToSign = "Message\n";
	    stringToSign += msg.getMessage() + "\n";
	    stringToSign += "MessageId\n";
	    stringToSign += msg.getMessageId() + "\n";
	    if (msg.getSubject() != null) {
	        stringToSign += "Subject\n";
	        stringToSign += msg.getSubject() + "\n";
	    }
	    stringToSign += "Timestamp\n";
	    stringToSign += msg.getTimestamp() + "\n";
	    stringToSign += "TopicArn\n";
	    stringToSign += msg.getTopicArn() + "\n";
	    stringToSign += "Type\n";
	    stringToSign += msg.getType() + "\n";
	    return stringToSign;
	}

	//Build the string to sign for SubscriptionConfirmation 
	//and UnsubscribeConfirmation messages.
	public static String buildSubscriptionStringToSign(Message msg) {
	    String stringToSign = null;
	    //Build the string to sign from the values in the message.
	    //Name and values separated by newline characters
	    //The name value pairs are sorted by name 
	    //in byte sort order.
	    stringToSign = "Message\n";
	    stringToSign += msg.getMessage() + "\n";
	    stringToSign += "MessageId\n";
	    stringToSign += msg.getMessageId() + "\n";
	    stringToSign += "SubscribeURL\n";
	    stringToSign += msg.getSubscribeURL() + "\n";
	    stringToSign += "Timestamp\n";
	    stringToSign += msg.getTimestamp() + "\n";
	    stringToSign += "Token\n";
	    stringToSign += msg.getToken() + "\n";
	    stringToSign += "TopicArn\n";
	    stringToSign += msg.getTopicArn() + "\n";
	    stringToSign += "Type\n";
	    stringToSign += msg.getType() + "\n";
	    return stringToSign;
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		log.info("Request:{}:{}",req.getLocalAddr(),req.getLocalPort());
		resp.setContentType("application/json");
		resp.setStatus(HttpServletResponse.SC_OK);
		try {
			resp.getWriter().println("{ \"Help\": \"Need to send a POST message for processing\"}");
			
		} catch (IOException e) {
			log.error("Error occured while processing the response : ",e);
		}
	}
}
