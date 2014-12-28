package com.jorge.thesis.io.database;

import com.jorge.thesis.datamodel.CEntityTagManager;
import com.jorge.thesis.io.file.FileReadUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.sql.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
                    TAGS_TABLE_KEY_TAG_NAME + " VARCHAR(32) PRIMARY KEY" +
                    " )");
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            //Should never happen
            throw new IllegalStateException("Error when preparing one of the commands for database environment setup." +
                    " Aborting.");
        }
        synchronized (DB_ACCESS_LOCK) {
            try {
                tagsTableCreation.execute();
            } catch (SQLException e) {
                if (!e.getSQLState().contentEquals("X0Y32")) {
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
            tagTableCreation = mConnection.prepareStatement("CREATE TABLE " + tagName +
                    " (" +
                    TAG_TABLE_KEY_SUBSCRIBER + " VARCHAR(32) PRIMARY KEY" +
                    " )");
            tagRowInsertion = mConnection.prepareStatement("INSERT INTO " + TAGS_TABLE_NAME + " VALUES ('" + tagName
                    + "')");
        } catch (SQLException e) {
            final String errorState = e.getSQLState();
            if (!errorState.contentEquals("X0Y32") || !errorState.contentEquals("23505")) {
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
                tagRowInsertion.execute();
            } catch (SQLException e) {
                if (!e.getSQLState().contentEquals("X0Y32")) {
                    e.printStackTrace(System.err);
                    throw new IllegalStateException("Unexpected error during tag insertion into database. Aborting.");
                }
            }
        }

        System.out.println("Added tag " + tagName + " to database.");

        return Boolean.TRUE;
    }

    /**
     * TODO Get ids currently registered for a tag
     */
    public List<String> getRegisteredIds(CEntityTagManager.CEntityTag tag) {
        synchronized (DB_ACCESS_LOCK) {
            List<String> ret = new LinkedList<>();

            System.out.println("Requested ids registered for tag " + tag.name() + ": " + ret.toString());
            return ret;
        }
    }

    /**
     * TODO Update registration id on all tags
     */
    public Boolean updateRegistrationIdOnAllTags(String oldId, String newId) {
        synchronized (DB_ACCESS_LOCK) {
            System.out.println("Replacing id " + oldId + " by new id " + newId);
            return Boolean.FALSE;
        }
    }

    /**
     * TODO Remove registration id on all tags
     */
    public Boolean removeRegistrationIdFromAllTags(String regId) {
        synchronized (DB_ACCESS_LOCK) {
            System.out.println("Removing registration id " + regId);
            return Boolean.FALSE;
        }
    }

    /**
     * TODO addSubscriptions
     *
     * @param deviceId
     * @param tagList
     * @return
     */
    public Boolean addSubscriptions(String deviceId, List<String> tagList) {
        synchronized (DB_ACCESS_LOCK) {
            return null;
        }
    }

    /**
     * TODO removeSubscriptions
     *
     * @param deviceId
     * @param tagList
     * @return
     */
    public Boolean removeSubscriptions(String deviceId, List<String> tagList) {
        synchronized (DB_ACCESS_LOCK) {
            return null;
        }
    }

    public List<String> getTagsNow() {
        PreparedStatement tagSelectionStatement;
        try {
            tagSelectionStatement = mConnection.prepareStatement("SELECT " + TAGS_TABLE_KEY_TAG_NAME + " FROM " +
                    TAGS_TABLE_NAME);
        } catch (SQLException e) {
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
                    ret.add(resultSet.getString(TAGS_TABLE_KEY_TAG_NAME));
                }
            } catch (SQLException e) {
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
}
