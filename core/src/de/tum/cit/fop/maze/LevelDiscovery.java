package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for finding available level files.
 *
 * Looks for {@code .properties} files in the {@code maps/} folder
 * and returns them as a sorted list. If a level exists both internally
 * and locally, the local version is used.
 */
public final class LevelDiscovery {

    /**
     * Private constructor to prevent creating instances.
     */
    private LevelDiscovery() {}

    /**
     * One entry per filename.
     * If a file exists in BOTH internal and local, LOCAL wins.
     */
    public static Array<FileHandle> listAllLevels() {
        Map<String, FileHandle> byName = new LinkedHashMap<>();

        FileHandle internalMaps = Gdx.files.internal("maps");
        if (internalMaps.exists() && internalMaps.isDirectory()) {
            for (FileHandle f : internalMaps.list(".properties")) {
                byName.put(f.name(), f); // lower priority
            }
        }

        FileHandle localMaps = Gdx.files.local("maps");
        if (localMaps.exists() && localMaps.isDirectory()) {
            for (FileHandle f : localMaps.list(".properties")) {
                byName.put(f.name(), f); // higher priority (overwrites)
            }
        }

        Array<FileHandle> out = new Array<>();
        for (FileHandle f : byName.values()) out.add(f);

        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }
}
