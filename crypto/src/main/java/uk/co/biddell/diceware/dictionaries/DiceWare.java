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

import android.content.Context;
import io.oversec.one.crypto.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public final class DiceWare {

    private final Random rand = new SecureRandom();

    private Dictionary dictionary;

    public enum LengthType {
        WORD_LENGTH,
        CHARACTER_LENGTH
    }

    public enum Type {
        PASSWORD("Password", LengthType.CHARACTER_LENGTH),
        MAXIMUM_SECURITY_PASSWORD("Password with maximum security", LengthType.CHARACTER_LENGTH),
        PASSPHRASE("Passphrase", LengthType.WORD_LENGTH),
        PASSPHRASE_EXTRA_SECURITY("Passphrase with extra security", LengthType.WORD_LENGTH),
        RANDOM_LETTERS_AND_NUMBERS("Random letters and numbers", LengthType.CHARACTER_LENGTH),
        RANDOM_DECIMAL_NUMBERS("Random decimal numbers", LengthType.CHARACTER_LENGTH),
        RANDOM_HEXADECIMAL_NUMBERS("Random hexadecimal numbers", LengthType.CHARACTER_LENGTH);

        private final String description;
        private final LengthType lengthType;

        Type(final String description, final LengthType lengthType) {
            this.description = description;
            this.lengthType = lengthType;
        }

        public String getDescription() {
            return description;
        }

        public LengthType getLengthType() {
            return lengthType;
        }
    }

    public DiceWare(Context ctx) throws IOException {
        InputStream is = ctx.getAssets().open(ctx.getString(R.string.diceware_dictionary));

        this.dictionary = new Dictionary(is);
        is.close();
    }


    public DiceWords getDiceWords(final Type type, final int length) {
        switch (type) {
            case PASSPHRASE:
                return createPassphrase(length, false);
            case PASSPHRASE_EXTRA_SECURITY:
                return createPassphrase(length, true);
            case PASSWORD:
                return createPassword(length);
            case MAXIMUM_SECURITY_PASSWORD:
                return createMaximumSecurityPassword(length);
            case RANDOM_LETTERS_AND_NUMBERS:
                return createRandomLettersAndNumbers(length);
            case RANDOM_DECIMAL_NUMBERS:
                return createRandomDecimalNumber(length);
            case RANDOM_HEXADECIMAL_NUMBERS:
                return createRandomHexadecimalNumber(length);
        }
        final DiceWords diceWords = new DiceWords();
        diceWords.append("This ");
        diceWords.append("shouldn't ");
        diceWords.append("happen.");
        return diceWords;
    }

    private String getDiceWord() {
        // Throw the dice 5 times and build up our selection criteria,
        final StringBuilder currentWord = new StringBuilder(5);
        for (int j = 0; j < 5; ++j) {
            currentWord.append(throwDie());
        }
        // Now get our actual dice word
        return dictionary.getWord(currentWord.toString());
    }

    @SuppressWarnings("FinalPrivateMethod")
    private final DiceWords createPassword(final int length) {
        final char[] passwordSpecialChars[] = {
                {
                        '!', '@', '#', '$', '%', '^'
                }, {
                '&', '*', '(', ')', '-', '='
        }, {
                '+', '[', ']', '{', '}', '\\'
        }, {
                '|', '`', ';', ':', '\'', '"'
        }, {
                '<', '>', '/', '?', '.', ','
        }, {
                '~', '_', '3', '5', '7', '9'
        }
        };
        // Build up a list of words until we have matched or exceeded the
        // requested password length
        int actualLength = 0;
        final ArrayList<String> words = new ArrayList<>();
        while (actualLength < length) {
            final String word = getDiceWord();
            words.add(word);
            actualLength += word.length();
        }
        // Now pick out a word to capitalise and a word to have a special
        // char randomly inserted. If we're truncating the last word, don't select 
        // it as the word to insert the special char into.
        final int specialCharWord = rand.nextInt(actualLength > length ? words.size() - 1 : words.size());
        final int capitaliseWord = rand.nextInt(words.size());
        final DiceWords diceWords = new DiceWords();
        for (int i = 0; i < words.size(); ++i) {
            String word = words.get(i);
            if (i == capitaliseWord) {
                // This is our special word where we capitalise the first char.
                final char[] wordChars = word.toCharArray();
                wordChars[0] = Character.toUpperCase(wordChars[0]);
                word = String.valueOf(wordChars);
            }
            if (i == specialCharWord) {
                // This is our special word. Pick a random char within the
                // word to be replaced with our random special char.
                // TODO - we should be using a dice roll to determine the letter within the word
                final int extraSecurityWordLetter = rand.nextInt(word.length());
                final char securityChar = passwordSpecialChars[throwDie() - 1][throwDie() - 1];
                final char[] wordChars = word.toCharArray();
                wordChars[extraSecurityWordLetter] = securityChar;
                word = String.valueOf(wordChars);
            }
            // If the word is too long we chop the end off.
            final int currentLength = diceWords.getLength();
            if ((currentLength + word.length()) > length) {
                word = word.substring(0, length - currentLength);
            }
            diceWords.append(word);
        }
        return diceWords;
    }

    @SuppressWarnings("FinalPrivateMethod")
    private final DiceWords createMaximumSecurityPassword(final int length) {
        final char[] passwordCharsRoll1Or2[] = {
                {
                        'A', 'B', 'C', 'D', 'E', 'F'
                }, {
                'G', 'H', 'I', 'J', 'K', 'L'
        }, {
                'M', 'N', 'O', 'P', 'Q', 'R'
        }, {
                'S', 'T', 'U', 'V', 'W', 'X'
        }, {
                'Y', 'Z', '0', '1', '2', '3'
        }, {
                '4', '5', '6', '7', '8', '9'
        }
        };
        final char[] passwordCharsRoll3Or4[] = {
                {
                        'a', 'b', 'c', 'd', 'e', 'f'
                }, {
                'g', 'h', 'i', 'j', 'k', 'l'
        }, {
                'm', 'n', 'o', 'p', 'q', 'r'
        }, {
                's', 't', 'u', 'v', 'w', 'x'
        }, {
                'y', 'z', '~', '_', ' ', ' '
        }, {
                ' ', ' ', ' ', ' ', ' ', ' '
        }
        };
        final char[] passwordCharsRoll5Or6[] = {
                {
                        '!', '@', '#', '$', '%', '^'
                }, {
                '&', '*', '(', ')', '-', '='
        }, {
                '+', '[', ']', '{', '}', '\\'
        }, {
                '|', '`', ';', ':', '\'', '"'
        }, {
                '<', '>', '/', '?', '.', ','
        }, {
                ' ', ' ', ' ', ' ', ' ', ' '
        }
        };
        final DiceWords diceWords = new DiceWords();
        for (int i = 0; i < length; ++i) {
            char letter;
            do {
                // Do while we don't get a space for this char as we don't
                // want spaces.
                final int firstRoll = throwDie(), secondRoll = throwDie(), thirdRoll = throwDie();
                if (firstRoll == 1 || firstRoll == 2) {
                    letter = passwordCharsRoll1Or2[thirdRoll - 1][secondRoll - 1];
                } else if (firstRoll == 3 || firstRoll == 4) {
                    letter = passwordCharsRoll3Or4[thirdRoll - 1][secondRoll - 1];
                } else {
                    letter = passwordCharsRoll5Or6[thirdRoll - 1][secondRoll - 1];
                }
            } while (letter == ' ');
            diceWords.append(letter);
        }
        return diceWords;
    }

    private DiceWords createPassphrase(final int numberOfWords, final boolean maximiseSecurity) {
        final char[] passphraseExtraSecurityChars[] = {
                {
                        '~', '!', '#', '$', '%', '^'
                }, {
                '&', '*', '(', ')', '-', '='
        }, {
                '+', '[', ']', '\\', '{', '}'
        }, {
                ':', ';', '"', '\'', '<', '>'
        }, {
                '?', '/', '0', '1', '2', '3'
        }, {
                '4', '5', '6', '7', '8', '9'
        }
        };
        final DiceWords diceWords = new DiceWords();
        int actualLength = 0;
        final ArrayList<String> words = new ArrayList<>(numberOfWords);
        for (int i = 0; i < numberOfWords; ++i) {
            final String word = getDiceWord();
            words.add(word);
            actualLength += word.length();
        }
        if (actualLength < 14) {
            // Less than 14 is not recommended according to diceware so throw
            // again, should probably try this a few times.
            return createPassphrase(numberOfWords, maximiseSecurity);
        } else {
            int extraSecurityWord = -1;
            if (maximiseSecurity) {
                // If we maximise security we replace a char in a word whose
                // index is selected here.
                extraSecurityWord = rand.nextInt(numberOfWords);
            }
            for (int i = 0; i < words.size(); ++i) {
                String word = words.get(i);
                if (i == extraSecurityWord) {
                    // This is our special word. Pick a random char within the
                    // word to be replaced with our random special char.
                    // TODO - we should be using a dice roll to determine the letter within the word
                    final int extraSecurityWordLetter = rand.nextInt(word.length());
                    final char securityChar = passphraseExtraSecurityChars[throwDie() - 1][throwDie() - 1];
                    final char[] wordChars = word.toCharArray();
                    wordChars[extraSecurityWordLetter] = securityChar;
                    word = String.valueOf(wordChars);
                }
                diceWords.append(word);
            }
        }
        return diceWords;
    }

    private DiceWords createRandomLettersAndNumbers(final int length) {
        final char[] chars[] = {
                {
                        'A', 'B', 'C', 'D', 'E', 'F'
                }, {
                'G', 'H', 'I', 'J', 'K', 'L'
        }, {
                'M', 'N', 'O', 'P', 'Q', 'R'
        }, {
                'S', 'T', 'U', 'V', 'W', 'X'
        }, {
                'Y', 'Z', '0', '1', '2', '3'
        }, {
                '4', '5', '6', '7', '8', '9'
        }
        };
        final DiceWords diceWords = new DiceWords();
        for (int i = 0; i < length; ++i) {
            diceWords.append(chars[throwDie() - 1][throwDie() - 1]);
        }
        return diceWords;
    }

    @SuppressWarnings("FinalPrivateMethod")
    private final DiceWords createRandomDecimalNumber(final int length) {
        final char[] digits[] = {
                {
                        '1', '2', '3', '4', '5', '*'
                }, {
                '6', '7', '8', '9', '0', '*'
        }, {
                '1', '2', '3', '4', '5', '*'
        }, {
                '6', '7', '8', '9', '0', '*'
        }, {
                '1', '2', '3', '4', '5', '*'
        }, {
                '6', '7', '8', '9', '0', '*'
        }
        };
        final DiceWords diceWords = new DiceWords();
        for (int i = 0; i < length; ++i) {
            char digit;
            do {
                digit = digits[throwDie() - 1][throwDie() - 1];
            } while (digit == '*');
            diceWords.append(digit);
        }
        return diceWords;
    }

    @SuppressWarnings("FinalPrivateMethod")
    private final DiceWords createRandomHexadecimalNumber(final int length) {
        final char[] digits[] = {
                {
                        '0', '1', '2', '3', '4', '5'
                }, {
                '6', '7', '8', '9', 'A', 'B'
        }, {
                'C', 'D', 'E', 'F', '0', '1'
        }, {
                '2', '3', '4', '5', '6', '7'
        }, {
                '8', '9', 'A', 'B', 'C', 'D'
        }, {
                'E', 'F', '*', '*', '*', '*'
        }
        };
        final DiceWords diceWords = new DiceWords();
        for (int i = 0; i < length; ++i) {
            char digit;
            do {
                digit = digits[throwDie() - 1][throwDie() - 1];
            } while (digit == '*');
            diceWords.append(digit);
        }
        return diceWords;
    }

    private int throwDie() {
        return rand.nextInt(6) + 1;
    }
}