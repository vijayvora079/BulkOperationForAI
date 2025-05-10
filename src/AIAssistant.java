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
    private static final String[] API_KEYS = {"AIzaSyDdh_DU1svQMbglwpCWD4px91O7qp2K8Mg", //vijay.patel.it09
                                                "AIzaSyApG8Vk1L5Fbkv3HbgEUrT4ca7ytX6iXYw", //sixmind
                                                "AIzaSyCLDvbXX9BtHl9mviX91somalvn548ECaA",  //vijay.m.java
                                                "AIzaSyDeFPQSe3RRK9UFJktPL6N570qfz-nTFRs", //rinkal.patel.it09
                                                "AIzaSyAgyuH7VXglYgTFictsjxLBL9QHXPmlTrw" //vijay.vora.photo.1@gmail.com
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

        HttpURLConnection connection = null;
        try {
            connection = sendQueryToAITool(query, currentKeySequence);
        }catch(Exception e) {
            //e.printStackTrace();
            System.out.println("Looks like it;s 429 (Too many requests) error, change key");

            try {
                connection = sendQueryToAITool(query, getNextKeySequence());
            } catch (Exception e1) {
                //e1.printStackTrace();
                System.out.println("Looks like it;s 429 (Too many requests) error, change key AGAIN last time");

                // If all keys are having rate limit error, Wait for 30second
               try {
                        System.out.println("--------- Waiting for 30 secs ------------");
                        Thread.sleep(0);
                } catch (InterruptedException e3) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    System.out.println("Thread was interrupted, failed to complete sleep");
                }

                try {
                    connection = sendQueryToAITool(query, getNextKeySequence());
                } catch (Exception e2) {
                    e2.printStackTrace();;
                    System.out.println("Looks like it;s something different than 429");
                    throw new RuntimeException(e2);
                }
            }
        }


        // Get the response code.
        int responseCode = connection.getResponseCode();

        // Read the response from the connection's input stream.
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // Handle different response codes.
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String responseJsonString = response.toString(); // Return the AI's response.
                String coreResponse = parseAIResponse(responseJsonString);
                return coreResponse;
            } else {
                return "Error: " + responseCode + " - " + response.toString(); // Return the error response.
            }
        } finally {
            connection.disconnect(); // Close the connection.
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

