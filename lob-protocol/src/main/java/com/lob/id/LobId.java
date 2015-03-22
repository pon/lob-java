package com.lob.id;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

import static com.lob.Util.checkNotNull;
import static com.lob.Util.checkValidHex;

public abstract class LobId {
    private final static String SEP = "_";
    private final static int ID_LENGTH = 20;

    protected final String id;

    protected LobId(final Prefix prefix, final String id) {
        checkIdentifierFor(prefix, id);
        this.id = id;
    }

    private void checkIdentifierFor(final Prefix prefix, final String s) {
        // Make sure the string isn't malformed
        final String[] sepString = checkNotNull(s).split(SEP);
        if (sepString == null || sepString.length != 2) {
            throw new IllegalArgumentException("string " + s + " is not a valid identifier!");
        }

        // Make sure the string is 20 chars
        if (s.length() != ID_LENGTH) {
            throw new IllegalArgumentException("string " + s + " is not " + ID_LENGTH + " characters long!");
        }

        // Make sure the prefix matches
        final Prefix parsedPrefix = Prefix.fromString(sepString[0]);
        if (parsedPrefix == null) {
            throw new IllegalArgumentException(sepString[0] + " is not a valid prefix!");
        }
        if (parsedPrefix != prefix) {
            throw new IllegalArgumentException("string " + s + " is not an identifier for " + prefix.name + "!");
        }

        // Make sure the random string is hex
        checkValidHex(sepString[1]);


        // If we make it here, everything is good!
    }

    @JsonValue
    public String value() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.id;
    }

    @Override
    public boolean equals(final Object anObject) {
        return id.equals(anObject);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    protected static enum Prefix {
        ADDRESS("adr", "address"),
        JOB("job", "job"),
        OBJECT("obj", "object");

        private final static Map<String, Prefix> stringPrefixMap = new HashMap<String, Prefix>();
        static {
            for (final Prefix prefix : Prefix.values()) {
                stringPrefixMap.put(prefix.prefix, prefix);
            }
        }

        private final String prefix;
        private final String name;

        private Prefix(final String prefix, final String name) {
            this.prefix = prefix;
            this.name = name;
        }

        private static Prefix fromString(final String s) {
            return stringPrefixMap.get(s);
        }

        @Override
        public String toString() {
            return this.prefix;
        }
    }

}
