package data.methods;

import com.fs.starfarer.api.Global;

public class GetMengString {
    public static String getString(String category, String id) {
        return Global.getSettings().getString(category, id);
    }
}
