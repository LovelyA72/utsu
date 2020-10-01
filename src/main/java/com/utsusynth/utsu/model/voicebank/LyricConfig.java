package com.utsusynth.utsu.model.voicebank;

import com.utsusynth.utsu.common.data.LyricConfigData;
import com.utsusynth.utsu.common.data.LyricConfigData.FrqStatus;

import java.io.File;

/**
 * Internal representation of the configuration of a single lyric in a voicebank. Parsed from an
 * oto.ini or oto_ini.txt file.
 */
public class LyricConfig implements Comparable<LyricConfig> {
    public static final String MAIN_CATEGORY = "Main";

    private final File pathToFile; // example: /Library/Iona.utau/C3/de.wav
    private final String fileName; // example: C3/de.wav
    private final String category; // example: C3
    private final String trueLyric; // example: de
    private double offset; // Time in wav file before note starts, in ms.
    private double consonant; // Time in wav file before consonant ends, in ms.
    private double cutoff; // Time in wav file before note ends, in ms.
    private double preutterance; // Number of ms that go before note officially starts.
    private double overlap; // Number of ms that overlap with previous note.

    /**
     * Used when reading from file.
     */
    public LyricConfig(
            File pathToVoicebank,
            File pathToFile,
            String trueLyric,
            String[] configValues) {
        this(
                pathToVoicebank,
                trueLyric,
                pathToFile.toPath().toAbsolutePath().subpath(
                        // Get part of pathToFile not in pathToVoicebank.
                        pathToVoicebank.toPath().toAbsolutePath().getNameCount(),
                        pathToFile.toPath().toAbsolutePath().getNameCount()).toString(),
                Double.parseDouble(configValues[0]),
                Double.parseDouble(configValues[1]),
                Double.parseDouble(configValues[2]),
                Double.parseDouble(configValues[3]),
                Double.parseDouble(configValues[4]));
    }

    /**
     * Used when converting LyricConfigData into a LyricConfig.
     */
    public LyricConfig(
            File pathToVoicebank,
            String trueLyric,
            String fileName,
            double... configValues) {
        assert (configValues.length == 5);
        this.pathToFile = pathToVoicebank.toPath().resolve(fileName).toFile();
        this.category = getCategory(fileName);
        this.fileName = fileName;
        this.trueLyric = trueLyric;
        this.offset = configValues[0];
        this.consonant = configValues[1];
        this.cutoff = configValues[2];
        this.preutterance = configValues[3];
        this.overlap = configValues[4];
    }

    public double getOffset() {
        return offset;
    }

    public double getConsonant() {
        return consonant;
    }

    public double getCutoff() {
        return cutoff;
    }

    public double getPreutterance() {
        return preutterance;
    }

    public double getOverlap() {
        return overlap;
    }

    public File getPathToFile() {
        return pathToFile;
    }

    public String getFilename() {
        return fileName;
    }

    public String getCategory() {
        return category;
    }

    public String getTrueLyric() {
        return trueLyric;
    }

    private static String getCategory(String fileName) {
        String category = new File(fileName).getParent();
        if (category == null) {
            category = MAIN_CATEGORY;
        }
        return category;
    }

    LyricConfigData getData(boolean hasFrq) {
        return new LyricConfigData(
                pathToFile,
                category,
                trueLyric,
                fileName,
                hasFrq ? FrqStatus.VALID.toString() : FrqStatus.INVALID.toString(),
                offset,
                consonant,
                cutoff,
                preutterance,
                overlap);
    }

    @Override
    public String toString() {
        return pathToFile + " " + offset + " " + consonant + " " + cutoff + " " + preutterance + " "
                + overlap;
    }

    @Override
    public int compareTo(LyricConfig other) {
        String thisLyric = fileName + trueLyric;
        String otherLyric = other.fileName + other.trueLyric;
        return thisLyric.compareTo(otherLyric);
    }

    public boolean equals(LyricConfig other) {
        return this.compareTo(other) == 0;
    }
}
