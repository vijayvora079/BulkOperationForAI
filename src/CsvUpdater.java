import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;

public class CsvUpdater {
    private static final String SEO_FIELDS_PENDING = "SEO_FIEDS_PENDING";

    public static void main(String[] args) {
        String inputFilePath = "products_export.csv";   // Input CSV
        String outputFilePath = "output.csv"; // Output CSV

        String titleField = "Title";
        String bodyHtmlField = "Body (HTML)";

        String seoTitleField = "SEO Title";
        String seoDescriptionField = "SEO Description";

        String tagField = "Tags";

        //String seoTitleAIPrefix = "Please suggest best Title for SEO for below product name. suggest only best option. No multiple suggestion, no explanation, only title. Here is original product name :";
        //String seoTitleAIPrefix = "My eCommerce store name is 'Lootfaat.com'. Please suggest me seo friendly meta title from below product details. suggest only best option. No multiple suggestion, no explanation, only title. Here is original product name :";
        //String seoDescriptionAIPrefix = "Now Please suggest best description (minimum 150 chars) for SEO from below product name and description . suggest only best option. No multiple suggestion, no explanation, only title. Here is original";

        String prompt_line_1 = "My eCommerce store name is 'Lootfaat.com'. below is my product details. ";
        String prompt_line_2 = " current product title : <CURRENT_PRODUCT_TITLE>" +  " Current product description : <CURRENT_PRODUCT_DESCRIPTION>";
        String prompt_line_3 = " Please suggest me product title, seo friendly meta title seo friendly meta description (minimum 150 chars). Strict instruction : 1. suggest only best option. No multiple suggestion, no explanation. 2. Dont use 'lootfaat' word in product title. 3. use '| Lootfaat.com' in SEO title only. 4. meta description / seo description should be max 150 chars 5. Need response in this format : <PRODUCT_TITLE></PRODUCT_TITLE> <SEO_TITLE></SEO_TITLE> <SEO_DESCRIPTION></SEO_DESCRIPTION>";
        String singleFullPrompt = prompt_line_1 + prompt_line_2 + prompt_line_3;

        // Clean previously generated output file
        File outputFile = new File(outputFilePath);
        if(outputFile.exists()){
            System.out.println("Deleting file : " + outputFilePath);
            outputFile.delete();
        }

        try (
                Reader reader = new FileReader(inputFilePath);
                CSVReader csvReader = new CSVReader(reader);
                Writer writer = new FileWriter(outputFilePath);
                CSVWriter csvWriter = new CSVWriter(writer)
        ) {
            List<String[]> allRows = csvReader.readAll();

            // Get column indexes
            String[] header = allRows.get(0);
            int titleIndex = -1;
            int bodyHtmlIndex = -1;
            int seoTitleIndex = -1;
            int seoDescIndex = -1;

            for (int i = 0; i < header.length; i++) {
                String col = header[i].trim();
                if (col.equalsIgnoreCase(titleField)) {
                    titleIndex = i;
                } else if (col.equalsIgnoreCase(bodyHtmlField)) {
                    bodyHtmlIndex = i;
                } else if (col.equalsIgnoreCase(seoTitleField)) {
                    seoTitleIndex = i;
                } else if (col.equalsIgnoreCase(seoDescriptionField)) {
                    seoDescIndex = i;
                }
            }

            if (titleIndex == -1 || bodyHtmlIndex == -1 || seoTitleIndex == -1 || seoDescIndex == -1) {
                System.out.println("Required columns not found!");
                return;
            }

            int productsProcessed = 0;
            // Update values for each row
            for (int i = 1; i < allRows.size() && productsProcessed < 100; i++) {
                String[] row = allRows.get(i);
                if (row.length > Math.max(Math.max(titleIndex, bodyHtmlIndex), Math.max(seoTitleIndex, seoDescIndex))) {
                    String titleValue = row[titleIndex];
                    String bodyHtmlValue = row[bodyHtmlIndex];
                    if(!titleValue.trim().equalsIgnoreCase("")) {
                        bodyHtmlValue = Jsoup.parse(bodyHtmlValue).text();
                        String fullPrompt = singleFullPrompt.replace("<CURRENT_PRODUCT_TITLE>", titleValue)
                                                           .replace("<CURRENT_PRODUCT_DESCRIPTION>", bodyHtmlValue)
                                                           .replace("\"", "");
                        System.out.println("Request sent to AI for SEO Title & Description: " + fullPrompt);
                        // Use the correct method to ensure API key is only changed on 429
                        String aiResponse = AIAssistant.sendRequestToAI(fullPrompt);
                        System.out.println("AI Response: " + aiResponse);
                        // Parse aiResponse for product title
                        String newProductTitle = "";
                        int startIdx = aiResponse.indexOf("<PRODUCT_TITLE>");
                        int endIdx = aiResponse.indexOf("</PRODUCT_TITLE>");
                        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                            newProductTitle = aiResponse.substring(startIdx + 15, endIdx).trim();
                        }
                        System.out.println("Parsed Product Title: " + newProductTitle);
                        // Parse aiResponse for SEO Title
                        String newSeoTitle = "";
                        startIdx = aiResponse.indexOf("<SEO_TITLE>");
                        endIdx = aiResponse.indexOf("</SEO_TITLE>");
                        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                            newSeoTitle = aiResponse.substring(startIdx + 11, endIdx).trim();
                        }
                        System.out.println("Parsed SEO Title: " + newSeoTitle);
                        // Parse aiResponse for SEO Description
                        String newSeoDescription = "";
                        startIdx = aiResponse.indexOf("<SEO_DESCRIPTION>");
                        endIdx = aiResponse.indexOf("</SEO_DESCRIPTION>");
                        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                            newSeoDescription = aiResponse.substring(startIdx + 17, endIdx).trim();
                        }
                        System.out.println("Parsed SEO Description: " + newSeoDescription);
                        // Update row with new titles and descriptions
                        row[titleIndex] = newProductTitle;
                        row[seoTitleIndex] = newSeoTitle;
                        row[seoDescIndex] = newSeoDescription;
                        // Add 'SEO_FIELDS_UPDATED' tag to tags field
                        int tagsIndex = -1;
                        for (int j = 0; j < header.length; j++) {
                            if (header[j].trim().equalsIgnoreCase("Tags")) {
                                tagsIndex = j;
                                break;
                            }
                        }
                        if (tagsIndex != -1) {
                            String tagsValue = row[tagsIndex];
                            if (tagsValue == null || tagsValue.trim().isEmpty()) {
                                row[tagsIndex] = "SEO_FIELDS_UPDATED";
                            } else {
                                // Replace SEO_FIELDS_PENDING with 'SEO_FIELDS_UPDATED'
                                row[tagsIndex] = tagsValue.replace(SEO_FIELDS_PENDING, "SEO_FIELDS_UPDATED");
                                // If SEO_FIELDS_PENDING was not present and 'SEO_FIELDS_UPDATED' is not present, append it
                                if (!tagsValue.contains(SEO_FIELDS_PENDING) && !tagsValue.contains("SEO_FIELDS_UPDATED")) {
                                    row[tagsIndex] = tagsValue + ", SEO_FIELDS_UPDATED";
                                }
                            }
                        }
                        productsProcessed++;
                        System.out.println("Products processed: " + productsProcessed);
                    }
                }
            }

            // Save updated CSV after all rows are processed
            csvWriter.writeAll(allRows);
            System.out.println("\nSEO Title and SEO Description updated successfully and saved to " + outputFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            // delete source file
            File inputFile = new File(inputFilePath);
            if(inputFile.exists()){
                System.out.println("Deleting file : " + inputFilePath);
                //inputFile.delete();
            }
        }
    }


}
