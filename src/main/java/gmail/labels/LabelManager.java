package gmail.labels;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


/**
 * A class of static methods that handles searching and creating of Gmail labels,
 * as well as modifying an email with a new label.
 */
public class LabelManager {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(LabelManager.class);

    /**
     * Creates a new label on a user's Gmail account.
     *
     * @param service The instance of Gmail to make requests from.
     * @param user The user's email address. The special value `me` can be used to indicate the authenticated user.
     * @param name The name of the label.
     * @return The ID of the new label.
     */
    public static String createLabel(Gmail service, String user, String name) {
        try {

            logger.debug("Creating new label with name: '{}'", name);
            // Apply settings to new label
            Label newLabel = new Label()
                    .setName(name)
                    .setLabelListVisibility("labelShow")
                    .setMessageListVisibility("show");
            String id = service.users().labels().create(user, newLabel).execute().getId();
            logger.debug("Label '{}' created with ID '{}'", name, id);
            return id;

        } catch (IOException e) {
            logger.error("Error occurred while attempting to create a new label!");
            logger.error(e.toString());
            throw new RuntimeException("Request to create new Gmail label failed");
        }
    }

    /**
     * Gets the label ID from the label with a matching name.
     * If no label with the name is found, null is returned.
     *
     * @param service The instance of Gmail to make requests from.
     * @param user The user's email address. The special value `me` can be used to indicate the authenticated user.
     * @param name The name of the label.
     * @return The ID of the label with a matching name. If no label is found, null is returned.
     */
    public static String getLabelId(Gmail service, String user, String name) {
        // Search for matching label in list of existing labels
        try {

            List<Label> labels = service.users().labels().list(user)
                    .execute()
                    .getLabels();
            logger.debug("Searching for label with matching name: '{}'", name);
            for (Label label : labels) {
                if (label.getName().equals(name)) {
                    String id = label.getId();
                    logger.debug("Label '{}' found with ID '{}'", name, id);
                    return id;
                }
            }

        } catch (IOException e) {
            logger.error("Error occurred while retrieving labels!");
            logger.error(e.toString());
            throw new RuntimeException("Request for Gmail labels failed");
        }

        // If label does not exist, return null
        logger.debug("No label found with name '{}'", name);
        return null;
    }

    /**
     * Applies a label to an email.
     *
     * @param service The instance of Gmail to make requests from.
     * @param user The user's email address. The special value `me` can be used to indicate the authenticated user.
     * @param labelId The ID of the label to add.
     * @param emailId The ID of the email to be modified.
     */
    public static void addLabelToEmail(Gmail service, String user, String labelId, String emailId) {
        try {

            // Create a new request to modify the email labels
            Gmail.Users.Messages.Modify addLabel = service.users().messages().modify(user, emailId,
                    new ModifyMessageRequest().setAddLabelIds(Collections.singletonList(labelId))
            );
            // Execute the request
            addLabel.execute();
            logger.debug("Label [{}] applied to email [{}]", labelId, emailId);

        } catch (IOException e) {
            logger.error("Error occurred while applying label [{}] to email [{}]!", emailId);
            logger.error(e.toString());
            throw new RuntimeException("Request to modify email labels failed");
        }
    }

}
