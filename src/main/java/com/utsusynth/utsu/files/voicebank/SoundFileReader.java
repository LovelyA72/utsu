package com.utsusynth.utsu.files.voicebank;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Optional;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.inject.Inject;
import com.utsusynth.utsu.common.StatusBar;
import org.apache.commons.io.FileUtils;
import com.utsusynth.utsu.common.data.FrequencyData;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.exception.ErrorLogger;

/**
 * Reads .frq and .wav files.
 */
public class SoundFileReader {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final StatusBar statusBar;

    @Inject
    public SoundFileReader(StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    /* TODO: Separate from frontend statusBar widget. */
    public Optional<FrequencyData> loadFrqData(File frqFile) {
        if (!frqFile.canRead()) {
            statusBar.setText("Warning: frq file not found: " + frqFile.getAbsolutePath());
            return Optional.empty();
        }
        try {
            // Parse header values.
            ByteBuffer buffer = ByteBuffer.wrap(FileUtils.readFileToByteArray(frqFile));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte[] charBuf = new byte[8];
            buffer.get(charBuf);
            if (!"FREQ0003".equals(new String(charBuf))) {
                statusBar.setText("Error: Tried to load frq data on a non-frq file.");
                return Optional.empty();
            }
            int samplesPerFrq = buffer.getInt(); // Number of samples per frequency value.
            double average = buffer.getDouble(); // Average F0 (pitch) of the sound.
            buffer.get(new byte[16]); // 16 bytes of empty space.

            // Parse frequency/amplitude values.
            int numBlocks = buffer.getInt();
            double[] frqs = new double[numBlocks];
            double[] amplitudes = new double[numBlocks];
            for (int i = 0; i < numBlocks; i++) {
                frqs[i] = buffer.getDouble();
                amplitudes[i] = buffer.getDouble();
            }
            if (buffer.hasRemaining()) {
                statusBar.setText("Warning: Parts of frq file were left unread.");
            }
            return Optional.of(new FrequencyData(average, samplesPerFrq, frqs, amplitudes));
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
            return Optional.empty();
        }
    }

    /* TODO: Separate from frontend statusBar widget. */
    public Optional<WavData> loadWavData(File wavFile) {
        if (!wavFile.canRead()) {
            statusBar.setText("Error: wav file not found!");
            return Optional.empty();
        }
        try (AudioInputStream input = AudioSystem.getAudioInputStream(wavFile)) {
            int numFrames = (int) input.getFrameLength();
            double lengthMs = numFrames / input.getFormat().getFrameRate() * 1000;
            if (input.getFormat().getSampleSizeInBits() != 16) {
                statusBar.setText("Error: Does not support sample sizes other than 16 bit.");
                return Optional.empty();
            }
            if (input.getFormat().getEncoding() != Encoding.PCM_SIGNED) {
                statusBar.setText("Error: Does not support encodings other than PCM_SIGNED.");
                return Optional.empty();
            }

            // Create a buffer to read 16-bit samples.
            byte[] bytes = new byte[numFrames * input.getFormat().getFrameSize()];
            int bytesRead = input.read(bytes);
            if (bytesRead < bytes.length) {
                statusBar.setText("Error: Could not read entire wav file.");
                return Optional.empty();
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            byteBuffer.order(
                    input.getFormat().isBigEndian() ? ByteOrder.BIG_ENDIAN
                            : ByteOrder.LITTLE_ENDIAN);
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

            int[] samples = new int[numFrames];
            for (int i = 0; i < numFrames; i++) {
                for (int channel = 0; channel < input.getFormat().getChannels(); channel++) {
                    int sample = shortBuffer.get();
                    if (channel == 0) {
                        samples[i] = sample;
                    }
                }
            }
            if (shortBuffer.hasRemaining()) {
                statusBar.setText("Warning: Parts of wav file were left unread.");
            }
            input.close();
            return Optional.of(new WavData(lengthMs, samples));
        } catch (IOException | UnsupportedAudioFileException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
            return Optional.empty();
        }
    }
}
