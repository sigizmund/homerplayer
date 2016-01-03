package com.studio4plus.homerplayer.model;

import android.os.Environment;
import android.util.Base64;

import com.studio4plus.homerplayer.util.DirectoryFilter;
import com.studio4plus.homerplayer.util.OrFilter;

import java.io.File;
import java.io.FileFilter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class FileScanner {

    static final String SAMPLE_BOOK_FILE_NAME = ".sample";

    private final String audioBooksDirectoryPath;

    @Inject
    public FileScanner(@Named("AUDIOBOOKS_DIRECTORY") String audioBooksDirectoryPath) {
        this.audioBooksDirectoryPath = audioBooksDirectoryPath;
    }

    public List<FileSet> scanAudioBooksDirectory() {
        List<FileSet> fileSets = new ArrayList<>();
        if (isExternalStorageReadable()) {
            File audioBooksDir = getDefaultAudioBooksDirectory();
            scanAndAppendBooks(audioBooksDir, fileSets);
            return fileSets;
        } else {
            return null;
        }
    }

    /**
     * Provide the default directory for audio books.
     *
     * The directory is in the devices external storage. Other than that there is nothing
     * special about it (e.g. it may be on an removable storage).
     */
    public File getDefaultAudioBooksDirectory() {
        File externalStorage = Environment.getExternalStorageDirectory();
        return new File(externalStorage, audioBooksDirectoryPath);
    }

    private void scanAndAppendBooks(File audioBooksDir, List<FileSet> fileSets) {
        if (audioBooksDir.exists() && audioBooksDir.isDirectory() && audioBooksDir.canRead()) {
            File[] audioBookDirs = audioBooksDir.listFiles(new DirectoryFilter());
            for (File directory : audioBookDirs) {
                FileSet fileSet = createFileSet(directory);
                if (fileSet != null)
                    fileSets.add(fileSet);
            }
        }
    }

    private FileSet createFileSet(File bookDirectory) {
        File[] allFiles = getAllAudioFiles(bookDirectory);
        String[] filePaths = new String[allFiles.length];
        int bookDirectoryPathLength = bookDirectory.getAbsolutePath().length();

        ByteBuffer bufferLong = ByteBuffer.allocate(Long.SIZE);
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (int i = 0; i < filePaths.length; ++i) {
                String path = allFiles[i].getAbsolutePath();
                String relativePath = path.substring(bookDirectoryPathLength);
                filePaths[i] = relativePath;

                // TODO: what if the same book is in two directories?
                bufferLong.putLong(0, allFiles[i].length());
                digest.update(relativePath.getBytes());
                digest.update(bufferLong);
            }
            String id = Base64.encodeToString(digest.digest(), Base64.NO_PADDING | Base64.NO_WRAP);
            if (filePaths.length > 0) {
                File sampleIndicator = new File(bookDirectory, SAMPLE_BOOK_FILE_NAME);
                boolean isDemoSample = sampleIndicator.exists();
                return new FileSet(id, bookDirectory, Arrays.asList(filePaths), isDemoSample);
            } else {
                return null;
            }
        } catch (NoSuchAlgorithmException e) {
            // Never happens.
            e.printStackTrace();
            throw new RuntimeException("MD5 not available");
        }
    }

    private File[] getAllAudioFiles(File directory) {
        List<File> files = new ArrayList<>();
        FileFilter audioFiles = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return isAudioFile(pathname);
            }
        };

        FileFilter filesAndDirectoriesFilter = new OrFilter(audioFiles, new DirectoryFilter());
        addFilesRecursive(directory, filesAndDirectoriesFilter, files);
        return files.toArray(new File[files.size()]);
    }

    private void addFilesRecursive(File directory, FileFilter filter, List<File> allFiles) {
        File[] files = directory.listFiles(filter);
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                addFilesRecursive(file, filter, allFiles);
            } else {
                allFiles.add(file);
            }
        }
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private static boolean isAudioFile(File file) {
        String fileName = file.getName();
        // TODO: allow other formats
        return fileName.toLowerCase().endsWith(".mp3");
    }

}
