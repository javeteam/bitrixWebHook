package com.aspect.web_hook.service;

import com.aspect.web_hook.entity.ExpandiLead;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class BitrixService {
    private final String bitrixRESTUrl;
    private final int bitrixUserId;
    private final Logger logger;

    @Autowired
    public BitrixService(@Value("${app.bitrixRESTUrl}") String bitrixRESTUrl, @Value("${app.bitrixUserId}") int bitrixUserId) {
        this.bitrixUserId = bitrixUserId;
        this.bitrixRESTUrl = bitrixRESTUrl;
        this.logger = LoggerFactory.getLogger(BitrixService.class);
    }

    public void sendNotification(ExpandiLead lead){
        Map<String, String> parameters = new HashMap<>();
        String message = "Lead " + lead.getContact().getFirstName() + " " + lead.getContact().getLastName() + " sent you a message: " + lead.getMessenger().getMessage();
        parameters.put("MESSAGE", message);
        parameters.put("DIALOG_ID", String.valueOf(bitrixUserId));
        String url_str = bitrixRESTUrl + "im.message.add.json" + getParamsString(parameters);
        execute(url_str);
    }

    public void addLead(ExpandiLead lead){
        Map<String, String> parameters = processLeadParameters(lead);
        String url_str = bitrixRESTUrl + "crm.lead.add.json" + getParamsString(parameters);
        execute(url_str);
    }

    private void execute(String url_str){
        try{
            URL url = new URL(url_str);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            connection.setSSLSocketFactory(socketFactory);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            if(connection.getResponseCode() != 200){
                String response = getJsonResponse(connection.getErrorStream());
                BitrixJsonError error = new ObjectMapper().readValue(response, BitrixJsonError.class);
                if(error != null) throw new IOException(error.error_description);
            }

        } catch (IOException ex){
            logger.error("Command execution failed",  ex);
        }
    }

    private Map<String, String> processLeadParameters(ExpandiLead lead){
        Map<String, String> leadParams = new HashMap<>();
        String firstName = lead.getMessenger().getPlaceholders().getFirstName();
        if(firstName == null || firstName.isBlank()) firstName = lead.getContact().getFirstName();
        leadParams.put("FIELDS[NAME]", firstName);

        String lastName = lead.getMessenger().getPlaceholders().getLastName();
        if(lastName == null || lastName.isBlank()) lastName = lead.getContact().getLastName();
        leadParams.put("FIELDS[LAST_NAME]", lastName);

        String leadName = (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) ? "lead_" + lead.getContact().getId() : firstName + " " + lastName;
        leadParams.put("FIELDS[TITLE]", leadName);

        String profileLink = lead.getMessenger().getPlaceholders().getProfileLink();
        if(profileLink == null || profileLink.isBlank()) profileLink = lead.getContact().getProfileLink();
        leadParams.put("FIELDS[UF_CRM_1521122139]", profileLink);

        String leadPosition = lead.getMessenger().getPlaceholders().getJobTitle();
        if(leadPosition == null || leadPosition.isBlank()) leadPosition = lead.getContact().getJobTitle();
        // Post... stupid russian idiots. There should be "position".
        leadParams.put("FIELDS[POST]", leadPosition);

        String companyName = lead.getMessenger().getPlaceholders().getCompanyName();
        if(companyName == null || companyName.isBlank()) companyName = lead.getContact().getCompanyName();
        leadParams.put("FIELDS[COMPANY_TITLE]", companyName);

        leadParams.put("FIELDS[SOURCE_DESCRIPTION]", lead.getMessenger().getCampaignInstance());
        leadParams.put("FIELDS[COMMENTS]", lead.getMessenger().getPlaceholders().getAchievement());

        String email = lead.getMessenger().getPlaceholders().getEmail();
        if(email == null || email.isBlank()) email = lead.getContact().getEmail();
        if(email != null && !email.isBlank()){
            leadParams.put("FIELDS[EMAIL][0][VALUE]", email);
            leadParams.put("FIELDS[EMAIL][0][VALUE_TYPE]", "WORK");
        }

        String phone = lead.getMessenger().getPlaceholders().getPhone();
        if(phone == null || phone.isBlank()) phone = lead.getContact().getPhone();
        if(phone != null && !phone.isBlank()){
            leadParams.put("FIELDS[PHONE][0][VALUE]", phone);
            leadParams.put("FIELDS[PHONE][0][VALUE_TYPE]", "WORK");
        }

        leadParams.put("FIELDS[ASSIGNED_BY_ID]", String.valueOf(bitrixUserId));
        leadParams.put("FIELDS[STATUS_ID]", "NEW");
        leadParams.put("FIELDS[SOURCE_ID]", "12");
        return leadParams;
    }



    private static String getParamsString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        result.append(("?"));

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            result.append("&");
        }

        return result.toString();
    }

    private String getJsonResponse(InputStream errorStream) throws IOException{
        StringBuilder res = new StringBuilder();
        InputStreamReader in = new InputStreamReader(errorStream);
        BufferedReader br = new BufferedReader(in);
        String output;
        while ((output = br.readLine()) != null) {
            res.append(output);
        }
        return res.toString();
    }

    private static class BitrixJsonError{
        private String error;
        private String error_description;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getError_description() {
            return error_description;
        }

        public void setError_description(String error_description) {
            this.error_description = error_description;
        }
    }
}
