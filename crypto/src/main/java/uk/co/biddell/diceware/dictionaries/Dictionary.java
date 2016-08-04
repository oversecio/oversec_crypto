/**
 * Copyright (C) 2014 Luke Biddell
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.biddell.diceware.dictionaries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dictionary {

    private final HashMap<String, String> lines = new HashMap<>(7777);

    public Dictionary(InputStream is) throws IOException {
        final Pattern p = Pattern.compile("^(\\d+)\\s+(\\S+)");
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = lnr.readLine()) != null) {
                final Matcher m = p.matcher(line);
                if (!m.find()) {
                    throw new RuntimeException("Unable to match line [" + line + "] in dictionary ");
                }
                lines.put(m.group(1), m.group(2));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (lnr != null) lnr.close();
        }
    }


    public long getEntropy(final int numberOfWords) {
        return BigInteger.valueOf(getWordCount()).pow(numberOfWords).bitLength();
    }

    public int getWordCount() {
        return lines.size();
    }

    public String getWord(final String diceRolls) {
        return lines.get(diceRolls);
    }
}