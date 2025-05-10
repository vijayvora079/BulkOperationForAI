import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;

import java.io.*;
import java.util.*;

public class CsvUpdater {

    public static void main(String[] args) {
        String inputFilePath = "products_export.csv";   // Input CSV
        String outputFilePath = "output.csv"; // Output CSV

        String titleField = "Title";
        String bodyHtmlField = "Body (HTML)";

        String seoTitleField = "SEO Title";
        String seoDescriptionField = "SEO Description";

        String seoTitleAIPrefix = "Please suggest best Title for SEO for below product name. suggest only best option. No multiple suggestion, no explanation, only title. Here is original product name :";
        String seoDescriptionAIPrefix = "Now Please suggest best description (minimum 150 chars) for SEO from below product name and description . suggest only best option. No multiple suggestion, no explanation, only title. Here is original";

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

            // Update values for each row
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                if (row.length > Math.max(Math.max(titleIndex, bodyHtmlIndex), Math.max(seoTitleIndex, seoDescIndex))) {
                    String titleValue = row[titleIndex];
                    String bodyHtmlValue = row[bodyHtmlIndex];

                    if(!titleValue.trim().equalsIgnoreCase("")) {
                        String aiQueryForSEOTitle  = seoTitleAIPrefix + titleValue;
                        aiQueryForSEOTitle = StringEscapeUtils.escapeHtml4(aiQueryForSEOTitle);
                        System.out.println("Request sent to AI for SEO Title Field : " + aiQueryForSEOTitle);
                        String seoTitleFromAI = AIAssistant.sendRequestToAI(aiQueryForSEOTitle);
                        row[seoTitleIndex] = seoTitleFromAI;

                        // Remove html code from body string
                        bodyHtmlValue = Jsoup.parse(bodyHtmlValue).text();

                        String aiQueryForSEODesc  = seoDescriptionAIPrefix + " Product Title : " + titleValue + ". Product Description : " + bodyHtmlValue;
                        aiQueryForSEODesc = StringEscapeUtils.escapeHtml4(aiQueryForSEODesc);
                        // Substring to max 256 chars
                        if(aiQueryForSEODesc.length() > 500){
                            aiQueryForSEODesc = aiQueryForSEODesc.substring(0, 499);
                        }
                        System.out.println("Request sent to AI for SEO Description Field : " + aiQueryForSEODesc);
                        String seoDescriptionFromAI = AIAssistant.sendRequestToAI(aiQueryForSEODesc);
                        row[seoDescIndex] = seoDescriptionFromAI;

                    }
                }
            }

            // Save updated CSV
            csvWriter.writeAll(allRows);
            System.out.println("\n SEO Title and SEO Description updated successfully and saved to " + outputFilePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            // delete source file
            File inputFile = new File(inputFilePath);
            if(inputFile.exists()){
                System.out.println("Deleting file : " + inputFilePath);
                inputFile.delete();
            }
        }
    }


}
