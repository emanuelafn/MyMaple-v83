package client.command;

import client.MapleCharacter;
import java.util.regex.Pattern;

public class MapleCharacterUtil {
	private static Pattern namePattern = Pattern.compile("[a-zA-Z0-9_-]{3,12}");

	private MapleCharacterUtil() {
		// whoosh
	}

	public static boolean canCreateChar(String name, int world) {
		if (!isNameLegal(name)) {
			return false;
		}
		if (MapleCharacter.getIdByName(name, world) != -1) {
			return false;
		}
		return true;
	}

	public static boolean isNameLegal (String name) {
		if (name.length() < 3 || name.length() > 12) {
			return false;
		}

		return namePattern.matcher(name).matches();
	}

	public static String makeMapleReadable(String in) {
		String wui = in.replace('I', 'i');
		wui = wui.replace('l', 'L');
		wui = wui.replace("rn", "Rn");
		wui = wui.replace("vv", "Vv");
		wui = wui.replace("VV", "Vv");
		return wui;
	}
}
