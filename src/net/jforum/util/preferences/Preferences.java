package net.jforum.util.preferences;

import java.util.prefs.BackingStoreException;

/**
 * A utility class to access Java's Preferences API
 */

public class Preferences {

	private static java.util.prefs.Preferences prefRoot = java.util.prefs.Preferences.userNodeForPackage(Preferences.class);

    public static boolean getBooleanValue (String key, boolean _default) {
        return prefRoot.getBoolean(key, _default);
    }

    public static int getIntValue (String key, int _default) {
        return prefRoot.getInt(key, _default);
    }

    public static String getStringValue (String key, String _default) {
        return prefRoot.get(key, _default);
    }

    public static void setValue (String key, boolean value) throws BackingStoreException {
        prefRoot.putBoolean(key, value);
		prefRoot.flush();
    }

    public static void setValue (String key, int value) throws BackingStoreException {
        prefRoot.putInt(key, value);
		prefRoot.flush();
	}

    public static void setValue (String key, String value) throws BackingStoreException {
        prefRoot.put(key, value);
		prefRoot.flush();
	}
}

