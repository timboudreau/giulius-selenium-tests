/*
 * The MIT License
 *
 * Copyright 2012 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.video;

import com.google.inject.Inject;
import com.mastfrog.settings.Settings;
import com.mastfrog.shutdown.hooks.ShutdownHookRegistry;
import com.mastfrog.util.time.TimeUtil;
import static com.mastfrog.video.VideoModule.log;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tim Boudreau
 */
final class FfmpegVideoRecorder implements VideoRecorder, Runnable {

    private Process process;
    private final Settings settings;

    @Inject
    @SuppressWarnings("LeakingThisInConstructor")
    FfmpegVideoRecorder(Settings settings, ShutdownHookRegistry reg) {
        reg.add(this);
        this.settings = settings;
        if (settings.getBoolean("record.video", false)) {
            start();
        } else {
            log("System property record.video is not set to true "
                    + "- will not record video");
        }
    }

    private String dateString() {
        return TimeUtil.toSortableStringFormat(ZonedDateTime.now());
    }

    @Override
    public void start() {
        String display = settings.getString("DISPLAY");
        if (display == null) {
            Logger.getLogger(FfmpegVideoRecorder.class.getName()).log(Level.SEVERE, null, new Error("ENV DISPLAY VARIABLE NOT SET"));
            return;
        }
        log("Starting ffmpeg");

        String filename = settings.getString("video");
        if (filename == null) {
            filename = settings.getString("testMethodQname", "screencast");
            String base = filename;
            if (!"screencast".equals(filename)) {
                filename = filename.replace('.', '-');
            }
            File f = new File("target");
            if (f.exists() && f.isDirectory()) {
                filename = "target" + File.separator + filename;
            }
            filename += '_' + dateString() + ".mp4";
        }
        int threads = settings.getInt("video.threads", 2);

        System.setProperty("video.file", filename);

        String cmdline = "ffmpeg -y -v 1 -r 15 -f x11grab -s 1280x1024 -i "
                + display + " -vcodec libx264 -threads " + threads + " -q:v 2 -r 30 " + filename;
        log("Will run ffmpeg with command-line: '" + cmdline + "'");
        log("Recording video to " + filename);
        String urlBase = settings.getString("base.video.url");
        if (urlBase != null) {
            if (!urlBase.endsWith("/")) {
                urlBase += "/";
            }
            urlBase += filename;

            String build = settings.getString("BUILD_NUMBER", "lastSuccessfulBuild");
            urlBase = urlBase.replace("_BUILD_", build);
            log("Video available from " + urlBase);
        }

        ProcessBuilder pb = new ProcessBuilder().inheritIO().command(cmdline.split("\\s"));
        pb.redirectOutput(Redirect.PIPE);
        pb.redirectError(Redirect.PIPE);
        try {
            synchronized (this) {
                process = pb.start();
            }
            log("Started ffmpeg");
        } catch (IOException ex) {
            Logger.getLogger(FfmpegVideoRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void stop() {
        run();
    }

    @Override
    public synchronized void run() {
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception ex) {
                Logger.getLogger(FfmpegVideoRecorder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
