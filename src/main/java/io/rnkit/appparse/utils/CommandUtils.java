package io.rnkit.appparse.utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Name: CommandUtils
 * Author: SimMan [liwei0990@gmail.com]
 * CreatedAt: 03/10/2017
 * Description:
 * Copyright (c) 2017 Toutoo, Inc.
 */
public class CommandUtils {

    private String pngdefryCommandPath = null;
    private String aaptCommandPath = null;

    public CommandUtils() throws Exception {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("mac")) {
            pngdefryCommandPath = "/bin/macosx-pngdefry";
            aaptCommandPath = "/bin/macosx-aapt";
        } else if (osName.toLowerCase().contains("linux")) {
            pngdefryCommandPath = "/bin/linux-pngdefry";
            aaptCommandPath = "/bin/linux-aapt";
        } else {
            throw new Exception("不支持的系统!");
        }
    }

    public String getCommandPath(String command) throws IOException, InterruptedException {
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), command);
        File commandFile = new File(path.toString());
        if (!commandFile.exists()) {
            InputStream inputStream = this.getClass().getResourceAsStream(command.equals("pngdefry") ? pngdefryCommandPath : aaptCommandPath);
            OutputStream os = new FileOutputStream(commandFile);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            inputStream.close();
            Runtime.getRuntime().exec("chmod 777 " + commandFile.getPath()).waitFor();
        }
        return commandFile.getPath();
    }
}