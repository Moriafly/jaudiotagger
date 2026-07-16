/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.audio;

import org.jaudiotagger.audio.aac.AacFileReader;
import org.jaudiotagger.audio.ape.ApeFileReader;
import org.jaudiotagger.audio.ape.ApeFileWriter;
import org.jaudiotagger.audio.aiff.AiffFileReader;
import org.jaudiotagger.audio.aiff.AiffFileWriter;
import org.jaudiotagger.audio.asf.AsfFileReader;
import org.jaudiotagger.audio.asf.AsfFileWriter;
import org.jaudiotagger.audio.dff.DffFileReader;
import org.jaudiotagger.audio.dsf.DsfFileReader;
import org.jaudiotagger.audio.dsf.DsfFileWriter;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.audio.flac.FlacFileWriter;
import org.jaudiotagger.audio.generic.*;
import org.jaudiotagger.audio.mp3.MP3FileReader;
import org.jaudiotagger.audio.mp3.MP3FileWriter;
import org.jaudiotagger.audio.mp4.Mp4FileReader;
import org.jaudiotagger.audio.mp4.Mp4FileWriter;
import org.jaudiotagger.audio.ogg.OggFileReader;
import org.jaudiotagger.audio.ogg.OggFileWriter;
import org.jaudiotagger.audio.ogg.OggOpusFileReader;
import org.jaudiotagger.audio.ogg.OggOpusFileWriter;
import org.jaudiotagger.audio.real.RealFileReader;
import org.jaudiotagger.audio.wav.WavFileReader;
import org.jaudiotagger.audio.wav.WavFileWriter;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The main entry point for Tag Reading/Writing operations.
 * <p>
 * This class serves as a factory/controller to select the appropriate reader/writer
 * based on the file extension (case ignored).
 * </p>
 * <p>
 * <b>Example Usage:</b>
 * <pre>{@code
 * // Reads the given file.
 * AudioFile audioFile = AudioFileIO.read(new File("audiofile.mp3"));
 *
 * // Retrieves the bitrate of the file.
 * int bitrate = audioFile.getBitrate();
 *
 * // Retrieve the artist name.
 * String artist = audioFile.getTag().getFirst(TagFieldKey.ARTIST);
 *
 * // Sets the genre to Prog. Rock. Note: the file on disk is unmodified at this stage.
 * audioFile.getTag().setGenre("Progressive Rock");
 *
 * // Write the modifications to the file on disk.
 * AudioFileIO.write(audioFile);
 * }</pre>
 * </p>
 * <p>
 * You can also use the {@code commit()} method defined for {@code AudioFile}s to achieve
 * the same goal as {@code AudioFileIO.write(File)}:
 * <pre>{@code
 * AudioFile audioFile = AudioFileIO.read(new File("audiofile.mp3"));
 * audioFile.getTag().setGenre("Progressive Rock");
 * audioFile.commit(); // Writes the modifications to the file on disk.
 * }</pre>
 * </p>
 *
 * @author Raphael Slinckx
 * @version $Id$
 * @see AudioFile
 * @see org.jaudiotagger.tag.Tag
 * @since v0.01
 */
@SuppressWarnings("unused")
public class AudioFileIO {
    // Logger
    private static final Logger logger = Logger.getLogger("org.jaudiotagger.audio");

    /**
     * This field contains the default instance for static use.
     */
    private static AudioFileIO defaultInstance;

    /**
     * This member is used to broadcast modification events to registered listeners.
     */
    private final ModificationHandler modificationHandler;

    // These maps contain all the readers/writers associated with extension as a key
    private final Map<String, AudioFileReader> readers = new HashMap<>();
    private final Map<String, AudioFileWriter> writers = new HashMap<>();

    /**
     * Creates an instance and initializes readers and writers.
     */
    public AudioFileIO() {
        this.modificationHandler = new ModificationHandler();
        prepareReadersAndWriters();
    }

    /**
     * Returns the default instance for static use.
     * <p>
     * This method is synchronized to ensure thread safety during initialization.
     * </p>
     *
     * @return The default AudioFileIO instance.
     */
    public static synchronized AudioFileIO getDefaultAudioFileIO() {
        if (defaultInstance == null) {
            defaultInstance = new AudioFileIO();
        }
        return defaultInstance;
    }

    // ===========================================================================
    // Static Convenience Methods
    // ===========================================================================

    /**
     * Read the tag contained in the given file using the specified extension.
     *
     * @param f   The file to read.
     * @param ext The extension to be used to identify the format.
     * @return The AudioFile with the file tag and the file encoding info.
     * @throws CannotReadException        If the file could not be read, the extension wasn't recognized, or an IO error occurred.
     * @throws IOException                If an I/O error occurs.
     * @throws TagException               If there is an error processing the tag.
     * @throws ReadOnlyFileException      If the file is read-only.
     * @throws InvalidAudioFrameException If the audio frame is invalid.
     */
    public static AudioFile readAs(File f, String ext)
            throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        return getDefaultAudioFileIO().readFileAs(f, ext);
    }

    /**
     * Read the tag contained in the given file by detecting magic numbers.
     *
     * @param f The file to read.
     * @return The AudioFile with the file tag and the file encoding info.
     * @throws CannotReadException        If the file could not be read or recognized.
     * @throws IOException                If an I/O error occurs.
     * @throws TagException               If there is an error processing the tag.
     * @throws ReadOnlyFileException      If the file is read-only.
     * @throws InvalidAudioFrameException If the audio frame is invalid.
     */
    public static AudioFile readMagic(File f)
            throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        return getDefaultAudioFileIO().readFileMagic(f);
    }

    /**
     * Read the tag contained in the given file by detecting the file extension.
     *
     * @param f The file to read.
     * @return The AudioFile with the file tag and the file encoding info.
     * @throws CannotReadException        If the file could not be read or recognized.
     * @throws IOException                If an I/O error occurs.
     * @throws TagException               If there is an error processing the tag.
     * @throws ReadOnlyFileException      If the file is read-only.
     * @throws InvalidAudioFrameException If the audio frame is invalid.
     */
    public static AudioFile read(File f)
            throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        return getDefaultAudioFileIO().readFile(f);
    }

    /**
     * Write the tag contained in the audioFile to the actual file on the disk.
     *
     * @param f The AudioFile to be written.
     * @throws CannotWriteException If the file could not be written/accessed.
     */
    public static void write(AudioFile f) throws CannotWriteException {
        getDefaultAudioFileIO().writeFile(f, null);
    }

    /**
     * Write the tag contained in the audioFile to the specified target path.
     *
     * @param f          The AudioFile to be written.
     * @param targetPath The file path to write to (without extension). Cannot be null.
     * @throws CannotWriteException If the file could not be written/accessed or the path is invalid.
     */
    public static void writeAs(AudioFile f, String targetPath) throws CannotWriteException {
        if (targetPath == null || targetPath.isEmpty()) {
            throw new CannotWriteException("Not a valid target path: " + targetPath);
        }
        getDefaultAudioFileIO().writeFile(f, targetPath);
    }

    /**
     * Delete the tag, if any, contained in the given file.
     *
     * @param f The file where the tag will be deleted.
     * @throws CannotReadException  If the file cannot be read.
     * @throws CannotWriteException If the file could not be written/accessed.
     */
    public static void delete(AudioFile f) throws CannotReadException, CannotWriteException {
        getDefaultAudioFileIO().deleteTag(f);
    }

    // ===========================================================================
    // Instance Methods
    // ===========================================================================

    /**
     * Adds a listener for all file formats.
     *
     * @param listener The listener to add.
     */
    public void addAudioFileModificationListener(AudioFileModificationListener listener) {
        this.modificationHandler.addAudioFileModificationListener(listener);
    }

    /**
     * Removes a listener for all file formats.
     *
     * @param listener The listener to remove.
     */
    public void removeAudioFileModificationListener(AudioFileModificationListener listener) {
        this.modificationHandler.removeAudioFileModificationListener(listener);
    }

    /**
     * Delete the tag, if any, contained in the given file.
     *
     * @param f The file where the tag will be deleted.
     * @throws CannotReadException  If the file cannot be read.
     * @throws CannotWriteException If the file could not be written/accessed, or the format isn't recognized.
     */
    public void deleteTag(AudioFile f) throws CannotReadException, CannotWriteException {
        String ext = Utils.getExtension(f.getFile());

        AudioFileWriter afw = writers.get(ext);
        if (afw == null) {
            throw new CannotWriteException(ErrorMessage.NO_DELETER_FOR_THIS_FORMAT.getMsg(ext));
        }

        afw.delete(f);
    }

    /**
     * Read the tag contained in the given file.
     *
     * @param f The file to read.
     * @return The AudioFile with the file tag and the file encoding info.
     * @throws CannotReadException        If no reader is found for the format.
     * @throws IOException                If an I/O error occurs.
     * @throws TagException               If there is an error processing the tag.
     * @throws ReadOnlyFileException      If the file is read-only.
     * @throws InvalidAudioFrameException If the audio frame is invalid.
     */
    public AudioFile readFile(File f)
            throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        String ext = Utils.getExtension(f);

        AudioFileReader afr = readers.get(ext);
        if (afr == null) {
            throw new CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext));
        }
        AudioFile tempFile = afr.read(f);
        tempFile.setExt(ext);
        return tempFile;
    }

    /**
     * Read the tag contained in the given file by detecting magic numbers.
     *
     * @param f The file to read.
     * @return The AudioFile with the file tag and the file encoding info.
     * @throws CannotReadException        If no reader is found for the format.
     * @throws IOException                If an I/O error occurs.
     * @throws TagException               If there is an error processing the tag.
     * @throws ReadOnlyFileException      If the file is read-only.
     * @throws InvalidAudioFrameException If the audio frame is invalid.
     */
    public AudioFile readFileMagic(File f)
            throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        String ext = Utils.getMagicExtension(f);

        AudioFileReader afr = readers.get(ext);
        if (afr == null) {
            throw new CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext));
        }

        AudioFile tempFile = afr.read(f);
        tempFile.setExt(ext);
        return tempFile;
    }

    /**
     * Read the tag contained in the given file using the specified extension.
     *
     * @param f   The file to read.
     * @param ext The extension to be used.
     * @return The AudioFile with the file tag and the file encoding info.
     * @throws CannotReadException        If no reader is found for the format.
     * @throws IOException                If an I/O error occurs.
     * @throws TagException               If there is an error processing the tag.
     * @throws ReadOnlyFileException      If the file is read-only.
     * @throws InvalidAudioFrameException If the audio frame is invalid.
     */
    public AudioFile readFileAs(File f, String ext)
            throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
        AudioFileReader afr = readers.get(ext);
        if (afr == null) {
            throw new CannotReadException(ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(ext));
        }

        AudioFile tempFile = afr.read(f);
        tempFile.setExt(ext);
        return tempFile;
    }

    /**
     * Write the tag contained in the audioFile in the actual file on the disk.
     *
     * @param f          The AudioFile to be written.
     * @param targetPath A file path without an extension, which provides a "save as". If null, normal "save".
     * @throws CannotWriteException If the file could not be written/accessed or no writer exists for the format.
     */
    public void writeFile(AudioFile f, String targetPath) throws CannotWriteException {
        String ext = f.getExt();

        if (targetPath != null && !targetPath.isEmpty()) {
            final File destination = new File(targetPath + "." + ext);
            try {
                Utils.copyThrowsOnException(f.getFile(), destination);
                f.setFile(destination);
            } catch (IOException e) {
                throw new CannotWriteException("Error While Copying: " + e.getMessage());
            }
        }

        AudioFileWriter afw = writers.get(ext);
        if (afw == null) {
            throw new CannotWriteException(ErrorMessage.NO_WRITER_FOR_THIS_FORMAT.getMsg(ext));
        }

        afw.write(f);
    }

    /**
     * Check if the file exists.
     *
     * @param file The file to check.
     * @throws FileNotFoundException If the file does not exist.
     */
    public void checkFileExists(File file) throws FileNotFoundException {
        logger.config("Reading file: path " + file.getPath() + ":abs:" + file.getAbsolutePath());
        if (!file.exists()) {
            logger.severe("Unable to find: " + file.getPath());
            throw new FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(file.getPath()));
        }
    }

    /**
     * Creates the readers and writers mapping.
     */
    private void prepareReadersAndWriters() {
        // Tag Readers
        readers.put(SupportedFileFormat.OGG.getFilesuffix(), new OggFileReader());
        readers.put(SupportedFileFormat.OGA.getFilesuffix(), new OggFileReader());
        readers.put(SupportedFileFormat.FLAC.getFilesuffix(), new FlacFileReader());
        readers.put(SupportedFileFormat.MP3.getFilesuffix(), new MP3FileReader());
        readers.put(SupportedFileFormat.MP4.getFilesuffix(), new Mp4FileReader());
        readers.put(SupportedFileFormat.M4A.getFilesuffix(), new Mp4FileReader());
        readers.put(SupportedFileFormat.M4P.getFilesuffix(), new Mp4FileReader());
        readers.put(SupportedFileFormat.M4B.getFilesuffix(), new Mp4FileReader());
        readers.put(SupportedFileFormat.WAV.getFilesuffix(), new WavFileReader());
        readers.put(SupportedFileFormat.WMA.getFilesuffix(), new AsfFileReader());
        readers.put(SupportedFileFormat.AIF.getFilesuffix(), new AiffFileReader());
        readers.put(SupportedFileFormat.AIFC.getFilesuffix(), new AiffFileReader());
        readers.put(SupportedFileFormat.AIFF.getFilesuffix(), new AiffFileReader());
        readers.put(SupportedFileFormat.AAC.getFilesuffix(), new AacFileReader());
        readers.put(SupportedFileFormat.DSF.getFilesuffix(), new DsfFileReader());
        readers.put(SupportedFileFormat.DFF.getFilesuffix(), new DffFileReader());
        readers.put(SupportedFileFormat.OPUS.getFilesuffix(), new OggOpusFileReader());
        readers.put(SupportedFileFormat.APE.getFilesuffix(), new ApeFileReader());

        RealFileReader realReader = new RealFileReader();
        readers.put(SupportedFileFormat.RA.getFilesuffix(), realReader);
        readers.put(SupportedFileFormat.RM.getFilesuffix(), realReader);

        // Tag Writers
        writers.put(SupportedFileFormat.OGG.getFilesuffix(), new OggFileWriter());
        writers.put(SupportedFileFormat.OGA.getFilesuffix(), new OggFileWriter());
        writers.put(SupportedFileFormat.FLAC.getFilesuffix(), new FlacFileWriter());
        writers.put(SupportedFileFormat.MP3.getFilesuffix(), new MP3FileWriter());
        writers.put(SupportedFileFormat.MP4.getFilesuffix(), new Mp4FileWriter());
        writers.put(SupportedFileFormat.M4A.getFilesuffix(), new Mp4FileWriter());
        writers.put(SupportedFileFormat.M4P.getFilesuffix(), new Mp4FileWriter());
        writers.put(SupportedFileFormat.M4B.getFilesuffix(), new Mp4FileWriter());
        writers.put(SupportedFileFormat.WAV.getFilesuffix(), new WavFileWriter());
        writers.put(SupportedFileFormat.WMA.getFilesuffix(), new AsfFileWriter());
        writers.put(SupportedFileFormat.AIF.getFilesuffix(), new AiffFileWriter());
        writers.put(SupportedFileFormat.AIFC.getFilesuffix(), new AiffFileWriter());
        writers.put(SupportedFileFormat.AIFF.getFilesuffix(), new AiffFileWriter());
        writers.put(SupportedFileFormat.DSF.getFilesuffix(), new DsfFileWriter());
        writers.put(SupportedFileFormat.OPUS.getFilesuffix(), new OggOpusFileWriter());
        writers.put(SupportedFileFormat.APE.getFilesuffix(), new ApeFileWriter());

        for (AudioFileWriter curr : writers.values()) {
            curr.setAudioFileModificationListener(this.modificationHandler);
        }
    }
}
