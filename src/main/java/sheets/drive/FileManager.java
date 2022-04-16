package sheets.drive;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class FileManager {

    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    /**
     * Creates a new file on a user's Google Drive under a given folder.
     *
     * @param service The instance of Drive to make requests from.
     * @param parentFolderId The parent folder in which the new file should be created.
     * @param name The name of the file.
     * @param mimeType The MimeType of the file.
     * @return The ID of the new file.
     */
    public static String createFile(Drive service, String parentFolderId, String name, String mimeType) {
        try {

            logger.debug("Creating new file with name: '{}'; MimeType: '{}'", name, mimeType);
            // Apply setting to new spreadsheet
            File fileMetadata = new File()
                    .setName(name)
                    .setMimeType(mimeType);
            if (parentFolderId != null) {
                fileMetadata.setParents(Collections.singletonList(parentFolderId));
            }

            String id = service.files().create(fileMetadata).setFields("id, parents").execute().getId();
            logger.debug("'{}' file called '{}' created with ID '{}'", mimeType, name, id);
            return id;

        } catch (IOException e) {
            logger.error("Error occurred while attempting to create new file!");
            logger.error(e.toString());
            throw new RuntimeException("Request to create new Drive file failed");
        }
    }

    /**
     * Gets the file ID from the file with a matching name.
     * If no file with the name is found, null is returned.
     *
     * @param service The instance of Drive to make requests from.
     * @param name The name of the file.
     * @param mimeType The MimeType of the file.
     * @return The ID of the file with a matching name. If no file is found, null is returned.
     */
    public static String getFileId(Drive service, String name, String mimeType) {
        // Search for matching file in list of existing files
        try {

            logger.debug("Searching for '{}' file with matching name: '{}'", mimeType, name);
            List<File> files = service.files().list()
                    .setQ(String.format("mimeType='%s' and name='%s'", mimeType, name))
                    .execute()
                    .getFiles();

            // Check if query produced results, and take first matching file
            if (files.size() > 0) {
                String id = files.get(0).getId();
                logger.debug("'{}' file called '{}' found with ID '{}'", mimeType, name, id);
                return id;
            }

        } catch (IOException e) {
            logger.error("Error occurred while searching for file: '{}'!", name);
            logger.error(e.toString());
            throw new RuntimeException("Request for Drive files failed");
        }

        // If file does not exist, return null
        logger.debug("No file found with name '{}'", name);
        return null;
    }

}
