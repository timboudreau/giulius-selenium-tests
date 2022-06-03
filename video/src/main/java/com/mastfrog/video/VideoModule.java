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

import com.google.inject.AbstractModule;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import java.io.IOException;

/**
 * Binds VideoRecorder
 *
 * @author Tim Boudreau
 */
public class VideoModule extends AbstractModule {

    @Override
    protected void configure() {
        // XXX bind something different on Windows?
        bind(VideoRecorder.class).to(FfmpegVideoRecorder.class).asEagerSingleton();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Settings settings = new SettingsBuilder("video")
                .add("record.video", "true")
                .addDefaultLocations()
                .parseCommandLineArguments(args).build();
        Dependencies deps = Dependencies.builder()
                .add(settings, "defaults")
                .add(new VideoModule())
                .build();

        VideoRecorder rec = deps.getInstance(VideoRecorder.class);
        rec.start();
        Thread.sleep(Long.MAX_VALUE);
    }

    static void log(CharSequence what) {
        if (Boolean.getBoolean("giulius.tests.verbose")) {
            System.err.println(what);
        }
    }
}
