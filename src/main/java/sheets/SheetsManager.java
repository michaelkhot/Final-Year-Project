package sheets;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import misc.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sheets.drive.FileManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static misc.CredentialManager.getCredentials;


/**
 * A class of static methods that handle the upload of financial reports.
 * This class accesses folders and sheets on the user's Google Drive, but only has access to this within the scope
 * of this project.
 */
public class SheetsManager {

    // Global objects
    private static final Logger logger = LoggerFactory.getLogger(SheetsManager.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /* MimeTypes for folder and spreadsheet file types */
    private static final String MIMETYPE_FOLDER = "application/vnd.google-apps.folder";
    private static final String MIMETYPE_SHEET = "application/vnd.google-apps.spreadsheet";
    /* Folder where report data is to be kept */
    private static final String FOLDER_NAME = "Financial Reports";

    /**
     * Uploads a given Report object to a dedicated folder of spreadsheets on Google Drive. Report CSVs are saved
     * under a root 'Financial Reports' folder, in a spreadsheet of data from the current year. Months within the
     * year are given their own sheets inside the spreadsheet. The report is only uploaded if it's data is not older
     * than the data already on the corresponding sheet.
     *
     * @param applicationName The identifier with which to make Drive/Sheets requests.
     * @param report The report to upload, with information on report date and CSV string.
     * @return Whether the report is successfully uploaded or not.
     * @throws GeneralSecurityException
     */
    public static boolean uploadReport(String applicationName, Report report) throws GeneralSecurityException {
        try {

            // Build new authorized API client services.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(applicationName)
                    .build();
            logger.info("Created new Drive instance");

            Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(applicationName)
                    .build();
            logger.info("Created new Sheets instance");

            // Get report folder ID
            String reportFolderId = getReportsFolder(driveService);
            // Get the parent spreadsheet grouping for the current report
            String spreadsheetId = getSpreadsheetReportGrouping(driveService, report, reportFolderId);
            // Get the specific sheet grouping for the current report
            Integer sheetId = getSheetReportGrouping(sheetsService, report, spreadsheetId);

            if (sheetDataIsOld(sheetsService, report, spreadsheetId)) {
                // Attach date metadata to start of CSV
                report.setCSV("Report date," + report.getEntireDate() + '\n' + report.getCSV());
                logger.debug("Attached current report date to top of CSV");

                // Upload CSV to the spreadsheet and sheet
                uploadCSV(sheetsService, report, spreadsheetId, sheetId);
                logger.info("Successfully uploaded report to Google Sheets, dated: {}", report.getEntireDate());
                return true;
            } else {
                logger.warn("Cancelled upload: report is not newer than existing data on spreadsheet");
                return false;
            }

        } catch (IOException e) {
            logger.error("Error occurred while uploading the report CSV!");
            logger.error(e.toString());
            throw new RuntimeException("Request to get latest report email failed");
        }
    }

    /**
     * Method used to get a sheet title from a report object.
     * Used for generating/finding a sheet title for a specific report.
     *
     * @param report The report to get the sheet name from.
     * @return The title of the sheet.
     */
    public static String getSheetNameFromReport(Report report) {
        return report.getMonthYear();
    }

    /**
     * Uploads the report CSV to the specified sheet ID in the specified spreadsheet ID.
     *
     * @param service The instance of Sheets to make requests from.
     * @param report The report to upload, with information on report date and CSV string.
     * @param spreadsheetId The spreadsheet to upload to.
     * @param sheetId The sheet within the spreadsheet to upload to.
     */
    private static void uploadCSV(Sheets service, Report report, String spreadsheetId, Integer sheetId) {
        try {

            // New paste data request
            GridCoordinate cell = new GridCoordinate()
                    .setSheetId(sheetId)
                    .setRowIndex(0)
                    .setColumnIndex(0);
            PasteDataRequest pasteDataRequest = new PasteDataRequest()
                    .setCoordinate(cell)
                    .setData(report.getCSV())
                    .setType("PASTE_VALUES")
                    .setDelimiter(",");
            List<Request> requests = new ArrayList<>();
            requests.add(new Request().setPasteData(pasteDataRequest));
            logger.debug("Set up request to paste CSV");

            // Add all requests as part of batch update request
            BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest()
                    .setRequests(requests);
            Sheets.Spreadsheets.BatchUpdate request = service.spreadsheets()
                    .batchUpdate(spreadsheetId, requestBody);
            // Execute request to add new sheet
            request.execute();
            logger.debug("Uploaded report CSV to spreadsheet [{}]: sheet [{}]", spreadsheetId, sheetId);

        } catch (IOException e) {
            logger.error("Error occurred while attempting to paste CSV to sheet!");
            logger.error(e.toString());
            throw new RuntimeException("Request to paste onto sheet failed");
        }
    }

    /**
     * Checks if the data on the sheet corresponding to the given report is older than the report's data.
     * This check is done using a cell at the top of sheet which holds the date of its current data.
     *
     * @param service The instance of Sheets to make requests from.
     * @param report The report used to find the corresponding sheet.
     * @param spreadsheetId The spreadsheet which holds the sheet of data.
     * @return Whether the sheet data is older than the report data.
     */
    private static boolean sheetDataIsOld(Sheets service, Report report, String spreadsheetId) {
        try {

            // Cell where the date of the last update is stored (in A1 notation)
            String dateCells = "A1:B1";
            // Get date of last report update
            String rangeParameter = getSheetNameFromReport(report) + '!' + dateCells;
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, rangeParameter).execute();
            List<List<Object>> values = response.getValues();
            logger.debug("Comparing report date against sheet date");

            // If sheet does not exist, return true always
            if (values == null) {
                logger.debug("Sheet is missing last report date");
                return true;
            }

            // Get value on first row (A), second column (2)
            String lastUpdated = values.get(0).get(1).toString();
            logger.debug("Last sheet report had date: {}", lastUpdated);

            // Parse report and sheet dates
            SimpleDateFormat parser = new SimpleDateFormat(Report.DATE_FORMAT);
            Date reportDate = parser.parse(report.getEntireDate());
            Date sheetDate = parser.parse(lastUpdated);

            return !reportDate.before(sheetDate);

        } catch (IOException e) {
            logger.error("Error occurred while retrieving sheet date!");
            logger.error(e.toString());
            throw new RuntimeException("Request to read cell value failed");
        } catch (ParseException e) {
            logger.error("Error occurred while comparing report and sheet dates!");
            logger.error(e.toString());
            throw new RuntimeException("Request to paste onto sheet failed");
        }
    }

    /**
     * Gets the root folder for reports. If it does not exist, then a new one is created and returned.
     *
     * @param service The instance of Drive to make requests from.
     * @return The file ID of the report folder.
     */
    private static String getReportsFolder(Drive service) {
        // Get ID of folder used to stored report spreadsheets
        String folderId = FileManager.getFileId(service, FOLDER_NAME, MIMETYPE_FOLDER);
        // If the folder does not exist, then create it
        if (folderId == null) {
            folderId = FileManager.createFile(service, null, FOLDER_NAME, MIMETYPE_FOLDER);
        }

        logger.debug("Reports folder [{}] retrieved", folderId);
        return folderId;
    }

    /**
     * Gets the ID of a spreadsheet with a matching name.
     * If no spreadsheet is found, a new one is created.
     *
     * @param service The instance of Drive to make requests from.
     * @param folderId The parent folder to create the spreadsheet if not found.
     * @param name The name of the spreadsheet.
     * @return The ID of the spreadsheet with a matching name. If no spreadsheet is found, a new one is returned.
     */
    private static String getSpreadsheet(Drive service, String folderId, String name) {
        // Get ID of spreadsheet
        String sheetId = FileManager.getFileId(service, name, MIMETYPE_SHEET);
        // If the spreadsheet does not exist, then create it
        if (sheetId == null) {
            sheetId = FileManager.createFile(service, folderId, name, MIMETYPE_SHEET);
        }

        logger.debug("Spreadsheet [{}] called '{}' retrieved", sheetId, name);
        return sheetId;
    }

    /**
     * Gets the ID of the corresponding spreadsheet for a given report. If a matching spreadsheet does not exist,
     * it is created via a call to getSpreadsheet. Spreadsheet report groups are calculated by taking the report year
     * and searching for a spreadsheet with a matching name of the 4-digit year.
     *
     * @param service The instance of Drive to make requests from.
     * @param report The report with which to find the corresponding spreadsheet.
     * @param folderId The parent folder to create a new spreadsheet if not found.
     * @return The spreadsheet ID for this report.
     */
    private static String getSpreadsheetReportGrouping(Drive service, Report report, String folderId) {
        // Get the current year from the financial report date
        String year = report.getYear();
        logger.debug("Searching for {} spreadsheet", year);
        return getSpreadsheet(service, folderId, year);
    }

    /**
     * Gets the sheet for a given report in a given spreadsheet ID. Sheet IDs are taken from the reporting month's
     * value minus one (Jan starts at 0, etc.). If a sheet with the corresponding ID does not exist, a new one is
     * created. At the same time, if the default sheet still exists it is deleted.
     *
     * @param service The instance of Sheets to make requests from.
     * @param report The report with which to find the corresponding sheet.
     * @param spreadsheetId The spreadsheet ID for the given report
     *                      (this can be called from getSpreadsheetReportingGroup)
     * @return The sheet ID for the report.
     */
    private static Integer getSheetReportGrouping(Sheets service, Report report, String spreadsheetId) {
        final String DEFAULT_SHEET_NAME = "Sheet1";
        Integer defaultSheetId = null;

        // Use the current month from the financial report date as an index (offset 1)
        Integer searchId = Integer.parseInt(report.getMonth()) - 1;

        try {

            // Search for the matching sheet under the spreadsheet with the given ID
            List<Sheet> sheets = service.spreadsheets().get(spreadsheetId)
                    .execute()
                    .getSheets();
            logger.debug("Searching for sheet with matching ID: '{}'", searchId);
            for (Sheet sheet : sheets) {
                Integer sheetId = sheet.getProperties().getSheetId();
                if (sheetId.equals(searchId)) {
                    logger.debug("Sheet with ID '{}' found", searchId);
                    return sheetId;
                } else if (sheet.getProperties().getTitle().equals(DEFAULT_SHEET_NAME)) {
                    logger.debug("Default sheet '{}' still exists", DEFAULT_SHEET_NAME);
                    defaultSheetId = sheetId;
                }
            }

        } catch (IOException e) {
            logger.error("Error occurred while retrieving spreadsheet data!");
            logger.error(e.toString());
            throw new RuntimeException("Request to get spreadsheet failed");
        }

        // If sheet grouping does not yet exist, create a new one
        try {

            logger.debug("Setting up request to add new sheet");
            // New sheet properties
            SheetProperties properties = new SheetProperties()
                    .setSheetId(searchId)
                    .setTitle(getSheetNameFromReport(report));

            // New add sheet request
            AddSheetRequest addSheetRequest = new AddSheetRequest()
                    .setProperties(properties);
            List<Request> requests = new ArrayList<>();
            requests.add(new Request().setAddSheet(addSheetRequest));

            // Add delete sheet request if default sheet still exists
            if (defaultSheetId != null) {
                logger.debug("Setting up request to delete default sheet");
                DeleteSheetRequest deleteSheetRequest = new DeleteSheetRequest()
                        .setSheetId(defaultSheetId);
                requests.add(new Request().setDeleteSheet(deleteSheetRequest));
            }

            // Add all requests as part of batch update request
            BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest()
                    .setRequests(requests);
            Sheets.Spreadsheets.BatchUpdate request = service.spreadsheets()
                    .batchUpdate(spreadsheetId, requestBody);

            // Execution request to add new sheet
            request.execute();
            logger.debug("Batch update requests executed");

            logger.debug("Created new sheet with ID: '{}'", searchId);
            return searchId;

        } catch (IOException e) {
            logger.error("Error occurred while attempting to add new sheet to spreadsheet!");
            logger.error(e.toString());
            throw new RuntimeException("Request to add sheet failed");
        }
    }

}
