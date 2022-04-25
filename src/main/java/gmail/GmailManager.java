package gmail;

import com.google.api.services.gmail.model.*;
import gmail.labels.LabelManager;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import misc.Report;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static misc.CredentialManager.getCredentials;


/**
 * A class of static methods that handle the retrieval of the latest PortfolioAnalyst Report via Gmail's API.
 */
public class GmailManager {

    // Global objects
    private static final Logger logger = LoggerFactory.getLogger(GmailManager.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /* Identifiers for PortfolioAnalyst report emails */
    private static final String EMAIL_SUBJECT = "PortfolioAnalyst Report";
    private static final String EMAIL_SENDER = "Interactive Brokers Client Services";
    /* Label used to categorise report emails that have already been processed */
    private static final String PROCESSED_LABEL = "Processed Reports";
    /* Date format in email subject: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html */
    private static final String EMAIL_SUBJECT_DATE_FORMAT = "MM/dd/yyyy";

    /* Email address upon which to make requests (`me` represents the authenticated user) */
    private static final String USER = "me";

    /**
     * Gets the latest report CSV from the Gmail inbox. Once a CSV is retrieved from an email, the email is labelled
     * as processed to prevent grabbing reports that have already been obtained.
     *
     * @param applicationName The identifier with which to make Gmail requests.
     * @return Report object containing the latest report CSV.
     * @throws GeneralSecurityException
     */
    public static Report getLatestReport(String applicationName) throws GeneralSecurityException {
        try {

            // Build a new authorized API client service.
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(applicationName)
                    .build();
            logger.info("Created new Gmail instance");

            // Set up label in case it does not exist
            String labelId = LabelManager.getLabelId(service, USER, PROCESSED_LABEL);
            if (labelId == null) {
                labelId = LabelManager.createLabel(service, USER, PROCESSED_LABEL);
            }

            // Get the email object of the latest report
            String emailId = getLatestReportEmailId(service);
            Message message = service.users().messages().get(USER, emailId).execute();

            // Get the date of the report
            String subject = getEmailSubject(message);
            String date = parseReportDate(subject);

            // Get report CSV
            String name = getAttachmentName(service, message);
            String data = getAttachmentString(service, message);
            if (data == null) {
                logger.error("Latest report email [{}] is missing report attachment!", date);
                throw new RuntimeException("Email does not have attachment");
            }

            // Label email as processed because report has been retrieved
            markEmailAsProcessed(service, emailId);

            Report report = new Report(name, Report.ReportType.UNKNOWN, date, data);
            logger.info("Retrieved latest unprocessed report, dated: {}", report.getEntireDate());
            return report;

        } catch (IOException e) {
            logger.error("Error occurred while retrieving the latest report email!");
            logger.error(e.toString());
            throw new RuntimeException("Request to get latest report email failed");
        }
    }

    /**
     * Gets the email ID from the latest PortfolioAnalyst report email.
     *
     * @param service The instance of Gmail to make requests from.
     * @return The ID of the most recent report email.
     */
    private static String getLatestReportEmailId(Gmail service) {
        try {

            logger.debug("Querying inbox for unprocessed emails");
            // Query inbox for emails with matching subjects and senders, which have not been labelled as processed
            Gmail.Users.Messages.List query = service.users().messages().list(USER).setQ(
                    String.format("(NOT label:%s) subject:(%s) from:(%s)", PROCESSED_LABEL, EMAIL_SUBJECT, EMAIL_SENDER)
            );
            ListMessagesResponse messagesResponse = query.execute();
            List<Message> messages = messagesResponse.getMessages();

            if (messages != null) {
                logger.debug("Found {} unprocessed report emails", messages.size());

                // This relies on ListMessagesResponse being ordered by date/time email arrives in inbox
                String latestId = messages.get(0).getId();
                logger.debug("Most recent email has ID {}", latestId);
                return latestId;
            } else {
                logger.error("No unprocessed emails were found!");
                throw new RuntimeException("Search query returned 0 unprocessed emails");
            }

        } catch (IOException e) {
            logger.error("Error occurred while retrieving a list of emails!");
            logger.error(e.toString());
            throw new RuntimeException("Request to get list of emails in inbox failed");
        }
    }

    /**
     * Modifies the email's labels by adding the processed label
     *
     * @param service The instance of Gmail to make requests from.
     * @param emailId The ID of the email to apply the processed label to.
     */
    private static void markEmailAsProcessed(Gmail service, String emailId) {
        // Get ID of label used to mark processed reports
        String processedLabelId = LabelManager.getLabelId(service, USER, PROCESSED_LABEL);
        // If the label does not exist, then create it
        if (processedLabelId == null) {
            processedLabelId = LabelManager.createLabel(service, USER, PROCESSED_LABEL);
        }

        LabelManager.addLabelToEmail(service, USER, processedLabelId, emailId);
        logger.debug("Email [{}] marked as processed", emailId);
    }

    /**
     * Gets the attachment filename from a given email.
     *
     * @param service The instance of Gmail to make requests from.
     * @param message The email to get the attachment from.
     * @return String of attachment name. If not attachment is found, null is returned.
     */
    private static String getAttachmentName(Gmail service, Message message) {
        MessagePart payload = message.getPayload();
        for (MessagePart part : payload.getParts()) {
            // Filename attribute only exists if this MessagePart represents an attachment
            if (!part.getFilename().trim().equals("")) {
                String name = part.getFilename();
                logger.debug("Attachment found: '{}'", name);
                return name;
            }
        }
        return null;
    }

    /**
     * Gets the attachment data from a given email.
     *
     * @param service The instance of Gmail to make requests from.
     * @param message The email to get the attachment from.
     * @return String of attachment data. If not attachment is found, null is returned.
     */
    private static String getAttachmentString(Gmail service, Message message) {
        MessagePart payload = message.getPayload();
        for (MessagePart part : payload.getParts()) {
            // Filename attribute only exists if this MessagePart represents an attachment
            if (!part.getFilename().trim().equals("")) {
                // Attachment data can be present in body data or in separate attachment
                if (part.getBody().getData() != null) {

                    logger.debug("Attachment data found");
                    return StringUtils.newStringUtf8(Base64.decodeBase64(part.getBody().getData()));

                } else {
                    try {

                        String attachmentId = part.getBody().getAttachmentId();
                        MessagePartBody attachment = service.users().messages()
                                .attachments().get(USER, message.getId(), attachmentId).execute();
                        logger.debug("Attachment data found in separate MessagePart");
                        return StringUtils.newStringUtf8(Base64.decodeBase64(attachment.getData()));

                    } catch (IOException e) {
                        logger.error("Error occurred while getting email attachment!");
                        logger.error(e.toString());
                        throw new RuntimeException("Request to get email attachment failed");
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the subject of an email.
     *
     * @param message The email to get the subject from
     * @return The subject string.
     */
    private static String getEmailSubject(Message message) {
        MessagePart payload = message.getPayload();
        for (MessagePartHeader header : payload.getHeaders()) {
            if (header.getName().equals("Subject")) {
                String subject = header.getValue();
                logger.debug("Email [{}] has subject: {}", message.getId(), subject);
                return subject;
            }
        }
        logger.error("Email is missing a subject header!");
        throw new RuntimeException("Subject field is missing from email headers");
    }

    /**
     * Parses the report date from the end of an email subject string.
     *
     * @param subject The email subject to parse the date from.
     * @return The reformatted report date, according to `DATE_FORMAT` defined in the Report class.
     */
    private static String parseReportDate(String subject) {
        try {

            // Check that subject is long enough to store a date
            if (subject.length() >= EMAIL_SUBJECT_DATE_FORMAT.length()) {
                // Get date from the end of the subject
                String dateInSubject = subject.substring(subject.length() - EMAIL_SUBJECT_DATE_FORMAT.length());

                DateFormat oldFormat = new SimpleDateFormat(EMAIL_SUBJECT_DATE_FORMAT, Locale.ENGLISH);
                DateFormat newFormat = new SimpleDateFormat(Report.DATE_FORMAT);

                Date date = oldFormat.parse(dateInSubject);
                String newDate = newFormat.format(date);
                logger.debug("Date {} reformatted to {}", dateInSubject, newDate);
                return newDate;
            }

            logger.error("Subject string: {} is too short to have a date!", subject);
            throw new IllegalArgumentException("Subject string is shorter than date format constant");

        } catch (ParseException e) {
            logger.error("Error occurred while parsing date from email subject: {}!", subject);
            logger.error(e.toString());
            throw new RuntimeException("Unable to parse report date from subject");
        }
    }

}
