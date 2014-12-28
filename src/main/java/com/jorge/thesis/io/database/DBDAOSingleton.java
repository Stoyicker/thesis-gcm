package com.jorge.thesis.io.database;

import com.jorge.thesis.datamodel.CEntityTagManager;
import com.jorge.thesis.io.file.FileReadUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public final class DBDAOSingleton {

    private static final Object LOCK = new Object();
    private static final Object DB_ACCESS_LOCK = new Object();
    private static final String TAGS_TABLE_NAME = "TAGS_TABLE";
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
                    "TAG_NAME CHAR PRIMARY KEY" +
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

    public void createTagsTables(List<String> tagNames) {
        List<String> cleanTagNames = new LinkedList<>();

        for (String tagName : tagNames) {
            tagName = tagName.toLowerCase().trim();
            if (!cleanTagNames.contains(tagName)) {
                cleanTagNames.add(tagName);
            }
        }

        for (String cleanTagName : cleanTagNames) {
            addTag(cleanTagName);
        }
    }

    /**
     * TODO Add a tag
     */
    public Boolean addTag(String tagName) {
        PreparedStatement tagRowInsertion, tagTableCreation;
        try {
            tagTableCreation = mConnection.prepareStatement("CREATE TABLE " + TAGS_TABLE_NAME +
                    " (" +
                    "TAG_NAME CHAR PRIMARY KEY" +
                    " )");
            tagRowInsertion = mConnection.prepareStatement("INSERT INTO " + TAGS_TABLE_NAME + " VALUES ('" + tagName
                    + "')");
        } catch (SQLException e) {
            e.printStackTrace(System.err);
            //Should never happen
            System.err.println("Error when preparing one of the commands for database tag insertion." +
                    " Aborting insertion.");
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
     * TODO Get all current tags
     */
    public List<String> getTagsNow() {
        synchronized (DB_ACCESS_LOCK) {
            List<String> ret = new LinkedList<>();

            System.out.println("Retrieved database tags " + ret.toString());
            return ret;
        }
    }
}
