package edu.vuamsterdam.MinimalConcepts;

import java.io.File;
import java.net.*;
import java.nio.file.Paths;

/**
 * A Get files more cleanly with Resources.getResource() and a relative path.
 * One of those things I have been copying over from project to project...
 */
public class Resources {
    public static File getResource(String relative) {
        URL res = Resources.class.getClassLoader().getResource(relative);
        try {
            return Paths.get(res.toURI()).toFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Cannot find file: " + relative);
        }
    }
}
