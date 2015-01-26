package com.jorge.thesis.io.database;

import com.jorge.thesis.datamodel.CEntityTagManager;
import com.jorge.thesis.io.file.FileReadUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public final class DBDAOSingleton {

    private static final Object LOCK = new Object();
    private static final Object DB_ACCESS_LOCK = new Object();
    private static final String TAGS_TABLE_NAME = "TAGS_TABLE";
    private static final String TAGS_TABLE_KEY_TAG_NAME = "TAG_NAME";
    private static final String TAG_TABLE_KEY_SUBSCRIBER = "TAG_SUBSCRIBER";
    private static volatile DBDAOSingleton mInstance;
    private final Connection mConnection;

    private DBDAOSingleton() {
        try {
            mConnection = DriverManager.getConnection(IOUtils.toString(FileReadUtils.class.getResourceAsStream
                    ("/database_connection_line")));
            mConnection.setAutoCommit(Boolean.TRUE);
        } catch (SQLException | IOException e) {
            e.printStackTrace(System.err);
            //Should never happen
            throw new IllegalStateException("Unable to initialize database connection. Aborting.");
        }
    }

    public static DBDAOSingleton getInstance() {
        DBDAOSingleton ret = mInstance;
        if (ret == null) {
            synchronized (LOCK) {
                ret = mInstance;
                if (ret == null) {
                    ret = new DBDAOSingleton();
                    mInstance = ret;
                }
            }
        }
        return ret;
    }

    public void createDatabaseEnvironment() {
        PreparedStatement tagsTableCreation;
        try {
            tagsTableCreation = mConnection.prepareStatement("CREATE TABLE " + TAGS_TABLE_NAME +
                    " (" +
                    TAGS_TABLE_KEY_TAG_NAME + " CHAR(254) PRIMARY KEY" +
                    " )");
        } catch (SQLException e) {
            final String errorState = e.getSQLState();
            System.err.println("ERROR STATE: " + errorState);
            e.printStackTrace(System.err);
            //Should never happen
            throw new IllegalStateException("Error when preparing one of the commands for database environment setup." +
                    " Aborting.");
        }
        synchronized (DB_ACCESS_LOCK) {
            try {
                tagsTableCreation.execute();
            } catch (SQLException e) {
                final String errorState = e.getSQLState();
                if (!e.getSQLState().contentEquals("X0Y32")) {
                    System.err.println("ERROR STATE: " + errorState);
                    e.printStackTrace(System.err);
                    throw new IllegalStateException("Unexpected error during database environment setup. Aborting.");
                }
                // If it already exists it's fine, just don't recreate it
            }
        }
    }

    public Boolean addTag(String tagName) {
        PreparedStatement tagRowInsertion, tagTableCreation;
        try {
            tagTableCreation = mConnection.prepareStatement("CREATE TABLE " + tagName.toUpperCase(Locale.ENGLISH) +
                    " (" +
                    TAG_TABLE_KEY_SUBSCRIBER + " CHAR(254) PRIMARY KEY" +
                    " )");
            tagRowInsertion = mConnection.prepareStatement("INSERT INTO " + TAGS_TABLE_NAME + " VALUES ('" + tagName
                    + "')");
        } catch (SQLException e) {
            final String errorState = e.getSQLState();
            if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505") && tableExists(tagName)) {
                System.err.println("ERROR STATE: " + errorState);
                e.printStackTrace(System.err);
                //Should never happen
                System.err.println("Error when preparing one of the commands for database tag insertion." +
                        " Aborting insertion.");
            }
            //If the tag is duplicated, no tag is added and therefore the operation fails
            return Boolean.FALSE;
        }

        synchronized (DB_ACCESS_LOCK) {
            try {
                tagTableCreation.execute();
            } catch (SQLException e) {
                final String errorState = e.getSQLState();
                if (!e.getSQLState().contentEquals("X0Y32") && !errorState.contentEquals("23505")) {
                    System.err.println("ERROR STATE: " + errorState);
                    e.printStackTrace(System.err);
                    throw new IllegalStateException("Unexpected error during tag insertion into database. Aborting.");
                } //If the table exists it's okay, just return control and continue
            }
            try {
                tagRowInsertion.execute();
            } catch (SQLException e) {
                final String errorState = e.getSQLState();
                if (!e.getSQLState().contentEquals("X0Y32") && !errorState.contentEquals("23505")) {
                    System.err.println("ERROR STATE: " + errorState);
                    e.printStackTrace(System.err);
                    throw new IllegalStateException("Unexpected error during tag insertion into database. Aborting.");
                } //If the table exists it's okay, just return
            }
        }

        System.out.println("Added tag " + tagName + " to database.");

        return Boolean.TRUE;
    }

    public List<String> getSubscribedRegistrationIds(CEntityTagManager.CEntityTag tag) {
        PreparedStatement idSelectionStatement;
        try {
            idSelectionStatement = mConnection.prepareStatement("SELECT " + TAG_TABLE_KEY_SUBSCRIBER + " FROM " +
                    tag.name());
        } catch (SQLException e) {
            if (tableExists(tag.name())) {
                e.printStackTrace(System.err);
                final String errorState = e.getSQLState();
                System.err.println("ERROR STATE: " + errorState);
                //Should never happen
                System.err.println("Error when preparing the command for retrieval of the subscribed ids for tag " + tag
                        .name() +
                        ". Returning empty collection of ids.");
            }
            System.err.println("During retrieval of the subscribed ids for tag " + tag.name() + ", it has been " +
                    "discovered that its table does not exist. Returning empty collection of ids.");
            return Collections.<String>emptyList();
        }

        final List<String> ret = new LinkedList<>();

        try {
            idSelectionStatement.execute();
            ResultSet resultSet = idSelectionStatement.getResultSet();
            while (resultSet.next()) {
                ret.add(resultSet.getString(TAG_TABLE_KEY_SUBSCRIBER));
            }
        } catch (SQLException e) {
            final String errorState = e.getSQLState();
            e.printStackTrace(System.err);
            System.err.println("ERROR STATE: " + errorState);
            //Should never happen
            System.err.println("Error when preparing the command for retrieval of subscribed it to tag" + tag.name() +
                    ". Returning empty collection of ids.");
            return Collections.<String>emptyList();
        }

        System.out.println("Registration ids " + ret.toString() + " are subscribed to " + tag.name());

        return ret;
    }

    /**
     * Assumes that the new id doesn't cause any conflicts. If it does,
     * it removes the old id.
     */
    public Boolean updateRegistrationIdOnAllTags(String oldId, String newId) {
        final List<String> allTags = getTagsNow();
        final List<PreparedStatement> tagRowUpdateCmds = new LinkedList<>();
        for (String tag : allTags) {
            try {
                if (tableExists(tag)) {
                    tagRowUpdateCmds.add(mConnection.prepareStatement("UPDATE " + tag + " SET " +
                            TAG_TABLE_KEY_SUBSCRIBER + " = '" +
                            newId + "' WHERE " +
                            TAG_TABLE_KEY_SUBSCRIBER + " IN" +
                            " (SELECT " + TAG_TABLE_KEY_SUBSCRIBER + " FROM " + tag + " WHERE " +
                            TAG_TABLE_KEY_SUBSCRIBER + "='" +
                            oldId + "')"));
                }
            } catch (SQLException e) {
                final String errorState = e.getSQLState();
                if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505") && tableExists(tag)) {
                    System.err.println("ERROR STATE: " + errorState);
                    e.printStackTrace(System.err);
                    //Should never happen
                    System.err.println("Unexpected error when preparing one of the commands for update of device " +
                            oldId + " " + "in database. Aborting update.");
                }
                //If the tag is duplicated, no tag is added and therefore the operation fails
            }
        }

        synchronized (DB_ACCESS_LOCK) {
            try {
                mConnection.setAutoCommit(Boolean.FALSE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when disabling autocommit for removal of device " +
                        oldId + " " +
                        " from database.");
            }
            for (PreparedStatement cmd : tagRowUpdateCmds) {
                try {
                    cmd.execute();
                } catch (SQLException e) {
                    final String errorState = e.getSQLState();
                    if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505")) {
                        System.err.println("ERROR STATE: " + errorState);
                        e.printStackTrace(System.err);
                        try {
                            mConnection.rollback();
                            throw new IllegalStateException("Unexpected error during update of device " + oldId +
                                    " " +
                                    "in database. Aborting update.");
                        } catch (SQLException e1) {
                            e1.printStackTrace(System.err);
                            //Should never happen
                            throw new IllegalStateException("Unexpected error during update of device " + oldId +
                                    " " +
                                    "in database. Aborting update failed.");
                        }
                    }
                }
            }
            try {
                mConnection.commit();
                mConnection.setAutoCommit(Boolean.TRUE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when restoring state during update of device " +
                        oldId + " " +
                        "in database.");
            }
        }

        System.out.println("Updated device " + oldId + " in database for " + newId + ".");

        return Boolean.TRUE;
    }

    public Boolean removeRegistrationIdFromAllTags(String deviceId) {
        final List<String> allTags = getTagsNow();
        final List<PreparedStatement> tagRowRemovalCmds = new LinkedList<>();
        for (String tag : allTags) {
            try {
                if (tableExists(tag)) {
                    tagRowRemovalCmds.add(mConnection.prepareStatement("DELETE FROM " + tag + " WHERE " +
                            TAG_TABLE_KEY_SUBSCRIBER + " IN" +
                            " (SELECT " + TAG_TABLE_KEY_SUBSCRIBER + " FROM " + tag + " WHERE " +
                            TAG_TABLE_KEY_SUBSCRIBER + "='" +
                            deviceId + "')"));
                }
            } catch (SQLException e) {
                final String errorState = e.getSQLState();
                if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505") && tableExists(tag)) {
                    System.err.println("ERROR STATE: " + errorState);
                    e.printStackTrace(System.err);
                    //Should never happen
                    System.err.println("Unexpected error when preparing one of the commands for removal of device " +
                            deviceId + " " + "from database. Aborting removal.");
                }
                //If the tag is duplicated, no tag is added and therefore the operation fails
            }
        }

        synchronized (DB_ACCESS_LOCK) {
            try {
                mConnection.setAutoCommit(Boolean.FALSE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when disabling autocommit for removal of device " +
                        deviceId + " " +
                        " from database.");
            }
            for (PreparedStatement cmd : tagRowRemovalCmds) {
                try {
                    cmd.execute();
                } catch (SQLException e) {
                    final String errorState = e.getSQLState();
                    if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505")) {
                        System.err.println("ERROR STATE: " + errorState);
                        e.printStackTrace(System.err);
                        try {
                            mConnection.rollback();
                            throw new IllegalStateException("Unexpected error during removal of device " + deviceId +
                                    " " +
                                    "from database. Aborting removal.");
                        } catch (SQLException e1) {
                            e1.printStackTrace(System.err);
                            //Should never happen
                            throw new IllegalStateException("Unexpected error during removal of device " + deviceId +
                                    " " +
                                    "from database. Aborting removal failed.");
                        }
                    }
                }
            }
            try {
                mConnection.commit();
                mConnection.setAutoCommit(Boolean.TRUE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when restoring state during removal of device " +
                        deviceId + " " +
                        "from database.");
            }
        }

        System.out.println("Removed device " + deviceId + " from database.");

        return Boolean.TRUE;
    }

    public Boolean addSubscriptions(String deviceId, List<String> tagList) {
        final List<PreparedStatement> tagRowInsertionCmds = new LinkedList<>();
        final List<String> existingTagNames = new LinkedList<>();
        try {
            for (String tag : tagList) {
                if (tableExists(tag)) {
                    tagRowInsertionCmds.add(mConnection.prepareStatement("INSERT INTO " + tag + " VALUES ('" +
                            deviceId +
                            "')"));
                    existingTagNames.add(tag);
                }
            }
        } catch (SQLException e) {
            final String errorState = e.getSQLState();
            if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505")) {
                System.err.println("ERROR STATE: " + errorState);
                e.printStackTrace(System.err);
                //Should never happen
                System.err.println("Error when preparing one of the commands for subscription of device " + deviceId
                        + " to tags " + tagList.toString() +
                        ". Aborting subscription.");
            }
            //If the tag is duplicated, no tag is added and therefore the operation fails
            return Boolean.FALSE;
        }

        synchronized (DB_ACCESS_LOCK) {
            try {
                mConnection.setAutoCommit(Boolean.FALSE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when disabling autocommit for subscription of " +
                        "device " +
                        deviceId + " " +
                        " to tags " + tagList.toString());
            }
            for (PreparedStatement cmd : tagRowInsertionCmds) {
                try {
                    cmd.execute();
                } catch (SQLException e) {
                    final String errorState = e.getSQLState();
                    if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505")) {
                        System.err.println("ERROR STATE: " + errorState);
                        e.printStackTrace(System.err);
                        try {
                            mConnection.rollback();
                            throw new IllegalStateException("Unexpected error during subscription of device " +
                                    deviceId
                                    + " " +
                                    "to tags " + tagList.toString() + " in the database. Aborting.");
                        } catch (SQLException e1) {
                            e1.printStackTrace(System.err);
                            //Should never happen
                            throw new IllegalStateException("Unexpected error during subscription of device " +
                                    deviceId
                                    + " " +
                                    "to tags " + tagList.toString() + " in the database. Aborting failed.");
                        }
                    }
                }
            }
            try {
                mConnection.commit();
                mConnection.setAutoCommit(Boolean.TRUE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when restoring state during subscription of " +
                        "device " +
                        "" + deviceId
                        + " " +
                        "to tags " + tagList.toString() + " in the database.");
            }
        }

        System.out.println("Added subscription of device " + deviceId + " to " + existingTagNames.toString() + " " +
                "to " +
                "database.");

        return Boolean.TRUE;
    }

    public Boolean removeSubscriptions(String deviceId, List<String> tagList) {
        final List<PreparedStatement> tagRowRemovalCmds = new LinkedList<>();
        final List<String> existingTagNames = new LinkedList<>();
        for (String tag : tagList) {
            try {
                if (tableExists(tag)) {
                    tagRowRemovalCmds.add(mConnection.prepareStatement("DELETE FROM " + tag + " WHERE " +
                            TAG_TABLE_KEY_SUBSCRIBER + " IN" +
                            " (SELECT " + TAG_TABLE_KEY_SUBSCRIBER + " FROM " + tag + " WHERE " +
                            TAG_TABLE_KEY_SUBSCRIBER + "='" +
                            deviceId + "')"));
                    existingTagNames.add(tag);
                }
            } catch (SQLException e) {
                final String errorState = e.getSQLState();
                if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505") && tableExists(tag)) {
                    System.err.println("ERROR STATE: " + errorState);
                    e.printStackTrace(System.err);
                    //Should never happen
                    System.err.println("Error when preparing one of the commands for unsubscription of device " +
                            deviceId
                            + " to tags " + tagList.toString() +
                            ". Aborting unsubscription.");
                }
                //If the tag is duplicated, no tag is added and therefore the operation fails
            }
        }

        synchronized (DB_ACCESS_LOCK) {
            try {
                mConnection.setAutoCommit(Boolean.FALSE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when disabling autocommit for unsubscription of " +
                        "device " +
                        deviceId + " " +
                        " from tags " + tagList.toString());
            }
            for (PreparedStatement cmd : tagRowRemovalCmds) {
                try {
                    cmd.execute();
                } catch (SQLException e) {
                    final String errorState = e.getSQLState();
                    if (!errorState.contentEquals("X0Y32") && !errorState.contentEquals("23505")) {
                        System.err.println("ERROR STATE: " + errorState);
                        e.printStackTrace(System.err);
                        try {
                            mConnection.rollback();
                            throw new IllegalStateException("Unexpected error during unsubscription of device " +
                                    deviceId
                                    + " " +
                                    "to tags " + tagList.toString() + " in the database. Aborting.");
                        } catch (SQLException e1) {
                            e1.printStackTrace(System.err);
                            //Should never happen
                            throw new IllegalStateException("Unexpected error during unsubscription of device " +
                                    deviceId
                                    + " " +
                                    "to tags " + tagList.toString() + " in the database. Aborting failed.");
                        }
                    }
                }
            }
            try {
                mConnection.commit();
                mConnection.setAutoCommit(Boolean.TRUE);
            } catch (SQLException e) {
                e.printStackTrace(System.err);
                //Should never happen
                throw new IllegalStateException("Unexpected error when restoring state during unsubscription of " +
                        "device " + deviceId
                        + " " +
                        "to tags " + tagList.toString() + " in the database. Aborting.");
            }
        }

        System.out.println("Added subscription of device " + deviceId + " to " + existingTagNames.toString() + " to " +
                "database.");

        return Boolean.TRUE;
    }

    public List<String> getTagsNow() {
        PreparedStatement tagSelectionStatement;
        try {
            tagSelectionStatement = mConnection.prepareStatement("SELECT " + TAGS_TABLE_KEY_TAG_NAME + " FROM " +
                    TAGS_TABLE_NAME);
        } catch (SQLException e) {
            final String errorState = e.getSQLState();
            System.err.println("ERROR STATE: " + errorState);
            e.printStackTrace(System.err);
            //Should never happen
            System.err.println("Error when preparing the command for retrieval of the full set of tags." +
                    " Returning empty collection of tags.");
            return Collections.<String>emptyList();
        }

        final List<String> ret = new LinkedList<>();

        synchronized (DB_ACCESS_LOCK) {
            try {
                tagSelectionStatement.execute();
                ResultSet resultSet = tagSelectionStatement.getResultSet();
                while (resultSet.next()) {
                    ret.add(resultSet.getString(TAGS_TABLE_KEY_TAG_NAME).trim());
                }
            } catch (SQLException e) {
                final String errorState = e.getSQLState();
                System.err.println("ERROR STATE: " + errorState);
                e.printStackTrace(System.err);
                //Should never happen
                System.err.println("Error when preparing the command for retrieval of the full set of tags." +
                        " Returning empty collection of tags.");
                return Collections.<String>emptyList();
            }

            System.out.println("Retrieved database tags " + ret.toString());
            return ret;
        }
    }

    private Boolean tableExists(String tableName) {
        DatabaseMetaData metadata;

        try {
            metadata = mConnection.getMetaData();
            ResultSet tableNames = metadata.getTables(null, null, null, new String[]{"TABLE"});
            while (tableNames.next()) {
                if (tableNames.getString("TABLE_NAME").toLowerCase(Locale.ENGLISH).contentEquals(tableName
                        .toLowerCase(Locale.ENGLISH)
                        .toLowerCase(Locale.ENGLISH))) {
                    return Boolean.TRUE;
                }
            }
        } catch (SQLException e) {
            final String errorCode = e.getSQLState();
            e.printStackTrace(System.err);
            System.err.println("ERROR STATE: " + errorCode);
            //Should never happen
            throw new IllegalStateException("Unexpected error when checking for existence of table " + tableName + "." +
                    " " +
                    "Aborting.");
        }

        return Boolean.FALSE;
    }
}
