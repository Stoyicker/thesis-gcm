package com.jorge.thesis.datamodel;

import com.jorge.thesis.gcm.GCMCommunicatorSingleton;
import com.jorge.thesis.io.database.DBDAOSingleton;
import com.jorge.thesis.io.enumrefl.EnumBuster;
import com.jorge.thesis.io.file.FileReadUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;

public abstract class CEntityTagManager {

    private static final String[] MINIMUM_TAG_SET = {}; //By default there are no tags
    private static final Object TAG_ACCESS_LOCK = new Object();
    private static Path DEFAULT_TAGS_FILE_PATH;

    private static void init() {
        synchronized (TAG_ACCESS_LOCK) {
            try {
                DEFAULT_TAGS_FILE_PATH = Paths.get(IOUtils.toString(CEntityTagManager.class.getResourceAsStream
                        ("/configuration_folder_name")), "tags.conf");
            } catch (IOException e) {
                e.printStackTrace(System.err);
                throw new IllegalStateException("Resource /configuration_folder_name not properly loaded.");
            }
        }
    }

    public static Boolean instantiateTagSet(Path... tagSetFilePaths) {
        synchronized (TAG_ACCESS_LOCK) {
            //Initialize
            init();

            //Load tags from configuration file or minimum tag set
            final EnumBuster<CEntityTag> buster =
                    new EnumBuster<>(CEntityTag.class,
                            CEntityTagManager.class);
            final Path tagSetFilePath = (tagSetFilePaths == null || tagSetFilePaths.length == 0) ?
                    DEFAULT_TAGS_FILE_PATH
                    : tagSetFilePaths[0];

            try {
                final List<String> tags = FileReadUtils.readCSVFile(tagSetFilePath);
                createTagsFromStringList(buster, tags);
            } catch (FileNotFoundException e) {
                System.out.println(MessageFormat.format("Tags file {0} not found. Loading default tags {1}",
                        tagSetFilePath.toAbsolutePath(), Arrays.toString(MINIMUM_TAG_SET)));
                instantiateDefaultTagSet(buster);
            }

            //Add externally-loaded tags to the database
            final CEntityTag[] values = CEntityTag.values();
            for (CEntityTag x : values)
                DBDAOSingleton.getInstance().addTag(x.name());

            //Load tags from the database
            createTagsFromStringList(buster, DBDAOSingleton.getInstance().getTagsNow());

            return CEntityTag.values().length > 0; //Weak success condition
        }
    }

    private static void instantiateDefaultTagSet(EnumBuster<CEntityTag> buster) {
        synchronized (TAG_ACCESS_LOCK) {
            createTagsFromStringList(buster, Arrays.asList(MINIMUM_TAG_SET));
        }
    }

    /**
     * @return {@link Integer} The amount of tags actually added.
     */
    public static Integer createTagsFromStringList(EnumBuster<CEntityTag> buster, List<String> tags) {
        synchronized (TAG_ACCESS_LOCK) {
            final List<String> nonDuplicateTags = new ArrayList<>();
            final Pattern tagFormatPattern = Pattern.compile("[a-z0-9_]+");
            for (String uniqueTag : tags) {
                uniqueTag = uniqueTag.trim().toLowerCase(Locale.ENGLISH);
                if (nonDuplicateTags.contains(uniqueTag) && !tagFormatPattern.matcher(uniqueTag).matches())
                    continue; //Eliminate duplicates or tags that don't come in the proper format
                nonDuplicateTags.add(uniqueTag);
            }

            int ret = 0;

            for (String tag : nonDuplicateTags) {
                if (!tag.isEmpty()) {
                    CEntityTag thisOneAsEntityTag = buster.make(tag);
                    buster.addByValue(thisOneAsEntityTag);
                    ret++;
                }
            }

            return ret;
        }
    }


    public static synchronized String generateAllCurrentTagsAsJSONArray() {
        synchronized (TAG_ACCESS_LOCK) {
            JSONObject ret = new JSONObject();
            JSONArray array = new JSONArray();

            array.put(CEntityTagManager.CEntityTag.values());

            try {
                ret.put("tags", array);
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                //Should never happen
            }

            return ret.toString();
        }
    }

    public static synchronized void createTagSyncRequest(String s) {
        synchronized (TAG_ACCESS_LOCK) {
            final EnumBuster<CEntityTagManager.CEntityTag> buster =
                    new EnumBuster<>(CEntityTagManager.CEntityTag.class,
                            CEntityTagManager.class);

            final List<String> l = new LinkedList<>();
            l.add(s);

            if (CEntityTagManager.createTagsFromStringList(buster, l) == 1) { //Return is amount of newly added tags
                if (DBDAOSingleton.getInstance().addTag(s)) {
                    GCMCommunicatorSingleton.getInstance().queueTagSyncRequest(CEntityTagManager.CEntityTag.valueOf(s));
                } else
                    System.out.println("Error when adding tag " + s + " to database. Skipping.");
            }
        }
    }

    public static synchronized List<String> getTagSubscribedRegistrationIds(CEntityTag tag) {
        return DBDAOSingleton.getInstance().getSubscribedRegistrationIds(tag);
    }

    public static Boolean subscribeRegistrationIdToTags(String deviceId, List<String> tags) {
        return DBDAOSingleton.getInstance().addSubscriptions(deviceId, tags);
    }

    public static Boolean unsubscribeRegistrationIdFromTags(String deviceId, List<String> tags) {
        return DBDAOSingleton.getInstance().removeSubscriptions(deviceId, tags);
    }

    public enum CEntityTag {}
}
