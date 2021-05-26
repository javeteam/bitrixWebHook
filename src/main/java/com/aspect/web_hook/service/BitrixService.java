package com.aspect.web_hook.service;

import com.aspect.web_hook.entity.ExpandiLead;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public void createTask(ExpandiLead lead){
        Map<String, String> parameters = new HashMap<>();

        parameters.put("fields[TITLE]", "Reply to lead " + lead.getContact().getFirstName() + " " + lead.getContact().getLastName());
        parameters.put("fields[DESCRIPTION]", lead.getMessenger().getMessage());
        parameters.put("fields[RESPONSIBLE_ID]", String.valueOf(bitrixUserId));
        parameters.put("fields[DEADLINE]", getFormattedTaskDeadline());
        parameters.put("fields[PRIORITY]", "2");

        long expandiLeadId = lead.getContact().getId();
        if(expandiLeadId > 0){
            String url_str = bitrixRESTUrl + "crm.lead.list.json?select[ID]&filter[UF_CRM_1622010050910]=" + expandiLeadId;
            String result = execute(url_str);
            if(result != null){
                try{
                    final ObjectNode node = new ObjectMapper().readValue(result, ObjectNode.class);
                    if(node.has("result")){
                        final JsonNode resultArray = node.get("result");
                        if(resultArray.isArray()){
                            for(int i = 0; i < resultArray.size(); i ++){
                                final JsonNode crmItem = resultArray.get(i);
                                if(crmItem.has("ID")){
                                    parameters.put("fields[UF_CRM_TASK][" + i + "]", crmItem.get("ID").textValue());
                                }
                            }
                        }
                    }
                } catch (IOException ex){
                    logger.info("Failed to find lead during task creation ",  ex);
                }
            }
        }
        String url_str = bitrixRESTUrl + "tasks.task.add.json" + getParamsString(parameters);
        execute(url_str);
    }

    public void addLead(ExpandiLead lead){
        long expandiLeadId = lead.getContact().getId();
        if(expandiLeadId > 0){
            String url_str = bitrixRESTUrl + "crm.lead.list.json?select[ID]&filter[UF_CRM_1622010050910]=" + expandiLeadId;
            String result = execute(url_str);
            if(result != null){
                try{
                    final ObjectNode node = new ObjectMapper().readValue(result, ObjectNode.class);
                    if(node.has("total")){
                        if(node.get("total").intValue() == 0){
                            Map<String, String> parameters = processLeadParameters(lead);
                            url_str = bitrixRESTUrl + "crm.lead.add.json" + getParamsString(parameters);
                            execute(url_str);
                        } else logger.info("Lead with id " + expandiLeadId + " already exist");
                    }
                } catch (IOException ex){
                    logger.error("Duplicates check failed ",  ex);
                }

            } else logger.error("Duplicates check for lead with id " + expandiLeadId + " failed");
        } else logger.info("Lead id (" + expandiLeadId + ") is wrong. Skipping...");
    }

    private String execute(String url_str){
        HttpsURLConnection connection = null;
        try{
            URL url = new URL(url_str);
            connection = (HttpsURLConnection) url.openConnection();
            SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            connection.setSSLSocketFactory(socketFactory);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            if(connection.getResponseCode() == 200){
                return getJsonResponse(connection.getInputStream());
            } else {
                String response = getJsonResponse(connection.getErrorStream());
                BitrixJsonError error = new ObjectMapper().readValue(response, BitrixJsonError.class);
                if(error != null) throw new IOException(error.error_description);
            }
        } catch (IOException ex){
            logger.error("Command execution failed",  ex);
        } finally {
            if(connection != null) connection.disconnect();
        }
        return null;
    }

    private Map<String, String> processLeadParameters(ExpandiLead lead){
        Map<String, String> leadParams = new HashMap<>();

        // expandi lead ID to check for duplicates
        leadParams.put("FIELDS[UF_CRM_1622010050910]", String.valueOf(lead.getContact().getId()));

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

    private String getJsonResponse(InputStream inputStream) throws IOException{
        StringBuilder res = new StringBuilder();
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(in);
        String output;
        while ((output = br.readLine()) != null) {
            res.append(output);
        }
        return res.toString();
    }

    private String getFormattedTaskDeadline(){
        LocalDateTime deadline = LocalDateTime.now();
        int dayOfWeekValue = deadline.getDayOfWeek().getValue();
        switch (dayOfWeekValue){
            case 1:
            case 2:
                deadline = deadline.plusDays(3);
                break;
            case 3:
                deadline = deadline.plusDays(2).withHour(18).withMinute(0);
                break;
            case 4:
            case 5:
                deadline = deadline.plusDays(5);
                break;
            case 6:
            case 7:
                deadline = deadline.plusDays(3 + 7 - dayOfWeekValue).withHour(18).withMinute(0);
        }
        return deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
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
