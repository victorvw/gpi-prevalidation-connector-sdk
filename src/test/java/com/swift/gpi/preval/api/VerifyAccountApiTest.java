/*
 * Beneficiary account pre-validation
 * Move your app forward with the Account Pre-Validation API
 *
 * OpenAPI spec version: 1.0.7-oas3
 *
 */

package com.swift.gpi.preval.api;

import com.swift.gpi.preval.ApiException;
import com.swift.gpi.preval.model.AccountVerificationRequest;
import com.swift.gpi.preval.model.AccountVerificationResponse1;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import java.util.*;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.IOException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * API tests for VerifyAccountApi
 */

public class VerifyAccountApiTest {

     public static String CRLF = "\r\n";
     public static String LAU_VERSION = "1.0";
     public static String APPL_ID = "BO2";
     public static String SIGNED = "(ApplAPIKey=yVGhKiV5z1ZGdaqFXoZ8AiSA9n5CrY6B),(x-bic=cclabeb0)";
     public static String LAUKEY = "Abcd1234Abcd1234Abcd1234Abcd1234";
     public static String ABSPATH = "/swift-preval-pilot/v1/accounts/verification";

    /**
     * Verify that a beneficiary account could be able to receive incoming funds.
     *
     * The service verifies that an account exists at the beneficiary bank and is capable of receiving incoming funds. This usually implies that the account is open, properly identified by the given number and, depending on the  jurisdiction and the market practices in use where the account is held, that the creditor name matches the name of the account holder. The service provider does not take liability for the response and does not provide any guarantee on the outcome of an actual transaction being sent to this account. The information provided is meant to be as accurate as possible at the time that the request was processed.  The requester must pass the creditor name and the service provider can use this information as part of the verification or not.
     *
     * @throws ApiException
     *          if the Api call fails
     */
    @Test
    public void verifyAccountTest() throws ApiException, ValidationException, IOException {

        String jsonStr1 = "{\"correlation_identifier\":\"SCENARIO1-CORRID-001\",\"context\":\"BENR\",\"uetr\":\"b916a97d-a699-4f20-b8c2-2b07e2684a27\",\"creditor_account\":\"GB3112000000001987426375\",\"creditor_name\":\"John Doe\",\"creditor_address\":{\"country\": \"GB\"},\"creditor_organisation_identification\":{\"any_bic\":\"BIC1GB51\"}}";

        JSONObject jsonSchema = new JSONObject(
                new JSONTokener(VerifyAccountApiTest.class.getResourceAsStream("/SWIFT-API-gpi-prevalidation-account-verification-request-1.0.7.json")));

        JSONObject jsonSubject = new JSONObject(
              new JSONTokener(VerifyAccountApiTest.class.getResourceAsStream("/request.json")));

        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);

        Gson g = new Gson();
        AccountVerificationRequest body = null;
        body = g.fromJson(jsonStr1, AccountVerificationRequest.class);
        String jsonStr = g.toJson(body);
        String laUApplicationID = APPL_ID;
        String laUVersion = "1.0";
        String laUCallTime = getDateTimeInZulu();
        String laURequestNonce = UUID.randomUUID().toString();
        String laUSigned = SIGNED;
        String xBic = "cclabeb0";
        String subjectDN = "o=cclausb0,o=swift" ;
        String institution = "cclausb0";
        String laUSignature = calculateLAU(laUApplicationID, laUCallTime,laURequestNonce,laUSigned, LAUKEY, ABSPATH, jsonStr);

        JSONObject requestSchemaFile = new JSONObject(
                new JSONTokener(VerifyAccountApiTest.class.getResourceAsStream("/SWIFT-API-gpi-prevalidation-account-verification-request-1.0.7.json")));

        JSONObject requestPayload = new JSONObject(
                new JSONTokener(VerifyAccountApiTest.class.getResourceAsStream("/request.json")));

        Schema requestSchema = SchemaLoader.load(requestSchemaFile);

        try{
            requestSchema.validate(requestPayload);
            System.out.println("Request payload validated against schema successfully!");
        } catch (ValidationException e){
            e.printStackTrace();
        }

        final VerifyAccountApi api = new VerifyAccountApi();

        AccountVerificationResponse1 response = api.verifyAccount(body, laUApplicationID, laUVersion, laUCallTime, laURequestNonce, laUSigned, laUSignature, xBic, subjectDN, institution);
        Gson gsonObj = new GsonBuilder().setPrettyPrinting().create();
        String json = gsonObj.toJson(response);

        JSONObject responseSchemaFile = new JSONObject(
                new JSONTokener(VerifyAccountApiTest.class.getResourceAsStream("/SWIFT-API-gpi-prevalidation-account-verification-response-1.0.7.json")));

        Schema responseSchema = SchemaLoader.load(responseSchemaFile);

        try{
            responseSchema.validate(new JSONObject(json));
            System.out.println("Response payload validated against schema successfully!");
        } catch (ValidationException e){
            e.printStackTrace();
        }
    }

    public static String getDateTimeInZulu(){
       return ZonedDateTime.now(ZoneOffset.UTC).toString();
   }

    public static String calculateLAU(String LAUApplicationID,
                                     String LAUCallTime, String LAURequestNonce, String LAUSigned,
                                     String LAUKey, String absPath, String requestBody)
   {
       try
       {
           Mac mac = Mac.getInstance("HmacSHA256");
           SecretKeySpec keyspec = new SecretKeySpec(
                   LAUKey.getBytes(Charset.forName("US-ASCII")), "HmacSHA256");
           mac.init(keyspec);
           StringBuilder sb = new StringBuilder(2048);
           sb.append("LAUApplicationID:").append(LAUApplicationID.trim()).append(CRLF);
           sb.append("LAUCallTime:").append(LAUCallTime.trim()).append(CRLF);
           sb.append("LAURequestNonce:").append(LAURequestNonce.trim()).append(CRLF);
           sb.append("LAUSigned:").append(LAUSigned.trim()).append(CRLF);
           sb.append("LAUVersion:").append(LAU_VERSION.trim()).append(CRLF);
           sb.append(absPath.trim()).append(CRLF);
           sb.append(requestBody);
           byte[] lau = mac.doFinal(sb.toString().getBytes("UTF-8"));
           byte[] lau_to_encode = new byte[16];
           System.arraycopy(lau, 0, lau_to_encode, 0, 16);
           String LAU = DatatypeConverter.printBase64Binary(lau_to_encode);
           return LAU;
       }
       catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException ex)
       {
           ex.printStackTrace();
           System.out.println(ex.getMessage());
       }
       return "";
   }
}
