package com.jorge.thesis.datamodel;

import com.jorge.thesis.io.FileReadUtils;
import com.jorge.thesis.io.enumrefl.EnumBuster;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CEntityTagClass {

    private static final String[] DEFAULT_TAGS = {"critical", "sickness", "promotions"};
    private static Path DEFAULT_TAGS_FILE_PATH;

    private CEntityTagClass() {
        try {
            DEFAULT_TAGS_FILE_PATH = Paths.get(IOUtils.toString(CEntityTagClass.class.getResourceAsStream
                    ("/configuration_folder_name")), "tags.conf");
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new IllegalStateException("Resource /configuration_folder_name not properly loaded.");
        }
    }

    public static Boolean instantiateTagSet(Path... tagSetFilePaths) {
        new CEntityTagClass(); //Init
        final EnumBuster<CEntityTag> buster =
                new EnumBuster<>(CEntityTag.class,
                        CEntityTagClass.class);
        final Path tagSetFilePath = (tagSetFilePaths == null || tagSetFilePaths.length == 0) ? DEFAULT_TAGS_FILE_PATH
                : tagSetFilePaths[0];

        try {
            final List<String> tags = FileReadUtils.readCSVFile(tagSetFilePath);
            createTagsFromStringList(buster, tags);
        } catch (FileNotFoundException e) {
            System.out.println(MessageFormat.format("Tags file {0} not found. Loading default tags {1}",
                    tagSetFilePath.toAbsolutePath(), Arrays.toString(DEFAULT_TAGS)));
            instantiateDefaultTagSet(buster);
        }

        return CEntityTag.values().length > 0;
    }

    private static void instantiateDefaultTagSet(EnumBuster<CEntityTag> buster) {
        createTagsFromStringList(buster, Arrays.asList(DEFAULT_TAGS));
    }

    private static void createTagsFromStringList(EnumBuster<CEntityTag> buster, List<String> tags) {
        final List<String> nonDuplicateTags = new ArrayList<>();
        for (String uniqueTag : tags) {
            uniqueTag = uniqueTag.trim().toLowerCase();
            if (nonDuplicateTags.contains(uniqueTag)) continue; //Eliminate duplicates
            nonDuplicateTags.add(uniqueTag);
        }

        for (String tag : nonDuplicateTags) {
            if (!tag.isEmpty()) {
                CEntityTag thisOneAsEntityTag = buster.make(tag);
                buster.addByValue(thisOneAsEntityTag);
            }
        }
    }

    public enum CEntityTag {}
}
