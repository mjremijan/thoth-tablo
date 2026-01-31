
package org.thoth.tablo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 *
 * @author Michael
 */
public class TabloExtractMain {

    static Path tablo = Path.of("D:\\Videos\\TV Shows\\Tablo");
    static Path rec = Path.of("T:\\rec");
    
    public static void main(String[] args) throws Exception {
     
        String DB_URL 
            = "jdbc:sqlite:D:/Desktop/db/Tablo.db";
        Connection conn
            = DriverManager.getConnection(DB_URL);
        PreparedStatement stmt
            = conn.prepareStatement("SELECT "
                    + " \"id\", \"title\", \"seasonNum\",\"episodeNum\", \"episodeTitle\""
                    + "FROM Recording "
                    + "WHERE "
                    + " \"title\" = ?"
                    + "AND \"objectType\" = 'recEpisode' "
                    + "AND \"actualDurationInSeconds\" > 0 ORDER BY id"
            );
        stmt.setString(1, "The Masked Singer");
        ResultSet rs
            = stmt.executeQuery();
        
        while (rs.next()) {
            // recording exist?
            Path recordingPath = getRecordingPath(rs.getInt("id"));
            
            // make season path
            Path seasonPath = makeSeasonPath(rs.getString("title"), rs.getInt("seasonNum"));
            
            // make the ts list file
            Path tsListPath = makeTsListFile(recordingPath);
            
            // build the episode file path
            Path episodeFilePath = makeEpisodeFilePath(seasonPath, rs.getString("title"), rs.getInt("seasonNum"), rs.getInt("episodeNum"), rs.getString("episodeTitle"));
            
            // make the mp4
            makeMp4(recordingPath, tsListPath, episodeFilePath);
        }        
    } 
    
    private static void makeMp4(Path recordingPath, Path tsListPath, Path episodeFilePath) throws Exception {
        // ffmpeg execution
        // ffmpeg -f concat -safe 0 -i list.txt -c copy output.mp4
        System.out.printf("- Generating mp4...%n");
        ProcessBuilder pb = new ProcessBuilder(
            "D:\\ffmpeg\\bin\\ffmpeg.exe",
            "-f", "concat",
            "-safe", "0",
            "-i", String.format("\"%s\"", tsListPath.toString()),
            "-c", "copy",
            String.format("\"%s\"", episodeFilePath.toString())
        );
        pb.redirectErrorStream(true);
        pb.directory(recordingPath.resolve("segs").toFile());
        System.out.println(String.join(" ", pb.command()));

        Process process = pb.start();
        // Drain output so the process can't block
        try (var r = process.inputReader()) {
            r.lines().forEach(System.out::println);
        }
        int exitCode = process.waitFor();
        System.out.printf("Exit code: %d%n", exitCode);        
    }    
    
    private static Path makeEpisodeFilePath(Path seasonPath, String showTitle, int season, int episode, String episodeTitle) {
        String filename = "";
        filename += showTitle;
        filename += " - ";
        filename += "s" + padded(season);
        filename += "e" + padded(episode);
        filename += " - ";
        filename += escapeForFilePath(episodeTitle);
        filename += ".mp4";
        
        // return
        Path episodeFilePath = seasonPath.resolve(filename);
        System.out.printf("Episode file path: %s%n", episodeFilePath);
        return episodeFilePath;
    }   
    
    private  static String escapeForFilePath(String input) {
        return input.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "");
    }

    private static Path makeTsListFile(Path recordingPath) throws Exception {
        // get list of ts file names
        Path segsPath = recordingPath.resolve("segs");
        File segsFile = segsPath.toFile();
        File[] files = segsFile.listFiles();
        for (int i=1; files == null; i++) {            
            Thread.sleep(100);
            files = segsFile.listFiles();
            System.out.printf("%s read attempt %d%n", segsFile.toString(), i);
        }
        for (File f : files) {
            System.out.printf("%s%n", f.getName());
        }
        
        
        // convert list to string
        String str = Arrays.asList(files).stream()
            .sorted()
            .map(f -> "file '" + segsPath.resolve(f.getName()).toString() + "'")
            .collect(Collectors.joining("\n"))
        ;

        
        // save to temporary file
        Path tsListPath = Files.createTempFile("tsfiles-", ".tmp");
        //tsListPath.toFile().deleteOnExit();
        Files.writeString(tsListPath, str);        

        
        // return
        System.out.printf("TS List file path: %s%n", tsListPath.toString());
        return tsListPath;
    }
    
    
    private static Path makeSeasonPath(String title, int season) throws Exception {       
        Path seasonPath
            = tablo.resolve(title).resolve(String.format("Season %s", padded(season)));
        Files.createDirectories(seasonPath);
        if (!Files.exists(seasonPath)) {
            throw new RuntimeException("Season path doesn't exist! " + seasonPath.toString());
        }
        System.out.printf("Season path: %s%n", seasonPath.toString());
        return seasonPath;
    }
    
    private static String padded(int s) {
        return String.format("%2s", String.valueOf(s)).replace(' ', '0');
    }
    
    static Path getRecordingPath(int id) throws Exception {
        Path recording = rec.resolve(String.valueOf(id));
        if (!Files.exists(recording)) {
            throw new RuntimeException("Path doesn't exist! " + recording.toString());
        }
        System.out.printf("Recording path: %s%n", recording.toString());
        return recording;
    } 


}
