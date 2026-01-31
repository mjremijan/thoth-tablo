
package org.thoth.tablo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Michael
 */
public class ThothTablo {

    public static void main(String[] args) throws Exception {
        
        String root = "D:\\Desktop\\abbott";
        List<Path> episodeDirs;
        
        try (var paths = Files.list(Path.of(root))) {
            episodeDirs = paths
                .filter(Files::isDirectory)
                .sorted()
                .collect(Collectors.toList())
            ;
        }
        
        System.out.printf("EPISODE LISTS%n%n");
        episodeDirs.forEach(System.out::println);
        episodeDirs.forEach(e -> saveToMp4(e));
    }
    
    static void saveToMp4(Path episodeDir) {
        try {
            // start
            System.out.printf("PROCESSING \"%s\"%n", episodeDir.toString());
            
            // process the json file
            var root = new ObjectMapper().readTree(
                new File(episodeDir.toFile(), "log/cr-in.json.raw")
            );
            
            // extract data from json
            var name = root.path("Name").asText();
            var seasonNum = String.format("%2s", root.path("SeasonNum").asText()).replace(' ', '0');
            var episodeNum = String.format("%2s", root.path("EpisodeNum").asText()).replace(' ', '0');
            
            // generate mp4 file name
            var filename = String.format("%s - s%se%s.mp4", name, seasonNum, episodeNum);
            System.out.printf("- \"%s\"%n", filename);
            
            // create ts file list
            String tsFileList =
                Files.list(Path.of(episodeDir.toString(), "segs"))
                    .filter(Files::isRegularFile)
                    .filter(t -> t.getFileName().toString().endsWith(".ts"))
                    .sorted()
                    .map( t -> "file '" + t.getFileName() + "'")
                    .collect(Collectors.joining("\n"))
            ;
            System.out.printf("- TS File List%n%s%n", tsFileList);
            
            // write ts file
            Files.writeString(Path.of(episodeDir.toString(), "segs/list.txt"), tsFileList);
              
            
            // ffmpeg execution
            // ffmpeg -f concat -safe 0 -i list.txt -c copy output.mp4
            System.out.printf("- Generating mp4...%n");
            ProcessBuilder pb = new ProcessBuilder(
                "D:\\ffmpeg\\bin\\ffmpeg.exe",
                "-f", "concat",
                "-safe", "0",
                "-i", "list.txt",
                "-c", "copy",
                String.format("\"%s\"", filename)
            );
            pb.redirectErrorStream(true);
            pb.directory(Path.of(episodeDir.toString(), "segs/").toFile());
            System.out.println(String.join(" ", pb.command()));
     
            Process process = pb.start();
            // Drain output so the process can't block
            try (var r = process.inputReader()) {
                r.lines().forEach(System.out::println);
            }
            int exitCode = process.waitFor();
            System.out.printf("Exit code: %d%n", exitCode);

                    
            // end
            System.out.printf("%n%n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
}
