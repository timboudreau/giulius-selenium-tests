/*
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
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

package com.mastfrog.testmain.suites;

import com.mastfrog.util.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public class SuiteLists {
    
    private final Map<String,Set<String>> typeNamesForSuite = new LinkedHashMap<>();

    public SuiteLists() throws IOException {
        InputStream[] inputs = Streams.locate(Suites.SUITES_FILE);
        if (inputs != null) {
            for (InputStream in : inputs) {
                try {
                    for (String line : Streams.readString(in).split("\n")) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        String[] suiteAndClassName = line.split(":", 2);
                        if (suiteAndClassName.length != 2) {
                            throw new IOException("Illegal content '" + line + "' in " + in);
                        }
                        Set<String> typeNames = typeNamesForSuite.get(suiteAndClassName[0]);
                        if (typeNames==null ) {
                            typeNames = new LinkedHashSet<>();
                            typeNamesForSuite.put(suiteAndClassName[0], typeNames);
                        }
                        typeNames.add(suiteAndClassName[1]);
                    }
                } finally {
                    in.close();
                }
            }
        }
    }
    
    public List<String> typeNames(String suite) {
        Set<String> all = typeNamesForSuite.get(suite);
        return all == null ? Collections.<String>emptyList() : new LinkedList<>(all);
    }
}
