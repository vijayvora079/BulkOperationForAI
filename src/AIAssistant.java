import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AIAssistant {

    // Placeholder API endpoint.  In a real application, this would be a Google Cloud API endpoint.
    private static final String API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="; // Replace with actual API endpoint
    private static final String[] API_KEYS = {
            "AIzaSyDzg6m5LqxjumENU7_f1lT4pY6X6Uv2DNI", // vijay.patel.ai.api.1
            "AIzaSyCw7sey7EFpn8gmFGvOTDQSi8Xp4ABo8z8", // vijay.patel.ai.api.2
            "AIzaSyCSOWJ91U_gz9tJf2SrFvP-4r6ElpnYHPI", // vijay.patel.ai.api.3
            "AIzaSyDGHhNQ2fFMoO4ZydufhrbaInG_1O8rCzc", // vijay.patel.ai.api.4
            "AIzaSyAo9BN4YL2nWyEhmsxsVG49kcl9zt83W50" // vijay.patel.ai.api.5
                                                };

    /**
     * Sends a request to the AI service and retrieves the response.
     *
     * @param query The user's query.
     * @return The AI's response, or an error message if the request fails.
     * @throws IOException If an error occurs during the HTTP request.
     */
    private static int currentKeySequence = 0;

    public static String sendRequestToAI(String query) throws IOException {
        int maxKeys = API_KEYS.length;
        while (true) {
            try {
                HttpURLConnection connection = sendQueryToAITool(query, currentKeySequence);
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        String responseJsonString = response.toString();
                        String coreResponse = parseAIResponse(responseJsonString);
                        connection.disconnect();
                        return coreResponse;
                    }
                } else if (responseCode == 429) {
                    System.out.println("429 (Too many requests) error, switching key");
                    connection.disconnect();
                    currentKeySequence++;
                    if (currentKeySequence == maxKeys) {
                        System.out.println("All keys exhausted. Waiting for 30 seconds before retrying from first key...");
                        try { Thread.sleep(30000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        currentKeySequence = 0;
                    }
                } else {
                    // Other error, just return error
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    connection.disconnect();
                    return "Error: " + responseCode + " - " + response.toString();
                }
            } catch (Exception e) {
                System.out.println("Error with key " + currentKeySequence + ": " + e.getMessage());
                currentKeySequence++;
                if (currentKeySequence == maxKeys) {
                    System.out.println("All keys exhausted due to exception. Waiting for 30 seconds before retrying from first key...");
                    try { Thread.sleep(30000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    currentKeySequence = 0;
                }
            }
        }
    }

    private static HttpURLConnection sendQueryToAITool(String query, int currentKeySequence) throws Exception {
        // Create the URL object.
        System.out.println("Using Key : " + API_KEYS[currentKeySequence]);
        URL url = new URL(API_ENDPOINT + API_KEYS[currentKeySequence]);

        // Open a connection to the URL.
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set the request method to POST.  Most AI APIs use POST for sending queries.
        connection.setRequestMethod("POST");

        // Set request headers.  These are crucial for authenticating and formatting the request.
        connection.setRequestProperty("Content-Type", "application/json");
        //connection.setRequestProperty("Authorization", "Bearer " + API_KEY); //  Include the API key
        connection.setDoOutput(true); // Enable output, as we're sending data.

        // Create the request body as a JSON string.
        //String requestBody = "{\"query\":\"" + query + "\"}"; //  Wrap the query in a JSON object
        //String requestBody = "{\"query\":\"" + query + "\"}"; //  Wrap the query in a JSON object
        String requestBody = "{\"contents\": [{\"parts\": [{\"text\": \""+ query +"\"}]}]}";

        // Write the request body to the connection's output stream.
        try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(requestBody);
            writer.flush();
        }

        try {
            // Get the response code.
            int statusCode = connection.getResponseCode();
            if(statusCode == 429){
                throw new Exception("Too Many Requests");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error while connecting to AI Tool" + e.getMessage());
            throw new Exception(e);
        }

        return connection;
    }

    // Placeholder method to simulate AI response parsing (replace with actual parsing logic)
    private static String parseAIResponse(String jsonResponse) {
        //  This is a placeholder.  In a real application, you would use a JSON library
        //  like org.json or Jackson to parse the JSON response from the AI service.
        //  The actual structure of the JSON response will depend on the API you are using.

        //  For this example, we'll just extract a dummy response.
        JSONObject jsonObject = new JSONObject(jsonResponse);
        System.out.println("Response : " + jsonObject);
        // Navigate through the JSON structure to get the "text" value
        JSONArray candidates = jsonObject.getJSONArray("candidates");
        JSONObject firstCandidate = candidates.getJSONObject(0); // Get the first element of the candidates array
        JSONObject content = firstCandidate.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        JSONObject firstPart = parts.getJSONObject(0);  // Get the first element of the parts array.
        String coreResponse = firstPart.getString("text");
        return coreResponse;
    }

    /**
     * Sends a request to the AI service, iterating through all API keys.
     * If all keys fail, waits 30 seconds and retries from the first key.
     * Repeats until a request succeeds.
     */
    public static String sendRequestWithRetry(String query) {
        int maxKeys = API_KEYS.length;
        while (true) {
            for (int i = 0; i < maxKeys; i++) {
                try {
                    HttpURLConnection connection = sendQueryToAITool(query, i);
                    int statusCode = connection.getResponseCode();
                    if (statusCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        return parseAIResponse(response.toString());
                    } else if (statusCode == 429) {
                        System.out.println("Key " + i + " rate limited. Trying next key...");
                        continue;
                    } else {
                        System.out.println("Key " + i + " failed with status: " + statusCode);
                        continue;
                    }
                } catch (Exception e) {
                    System.out.println("Key " + i + " failed: " + e.getMessage());
                    continue;
                }
            }
            System.out.println("All keys failed. Waiting 30 seconds before retrying...");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    // For Internal Testing
    public static void main(String[] args) {
        try {
            // Example usage
            String userQuery = "What is the capital of France?";
            String aiResponse = sendRequestToAI(userQuery);
            System.out.println("User Query: " + userQuery);
            System.out.println("AI Response: " + aiResponse);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static int getNextKeySequence(){
        currentKeySequence = currentKeySequence + 1;
        if(currentKeySequence >= API_KEYS.length){
            currentKeySequence = 0; // If there are no new key left, start from first key
        }
        return currentKeySequence;
    }
}
