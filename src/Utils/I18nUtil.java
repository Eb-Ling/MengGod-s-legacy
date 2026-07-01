package data.Utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.TextPanelAPI;

import java.awt.*;

/**
 * Modified from Originem's CMC by Nitori_Tachyon and homejerry99
 */
public class I18nUtil {
    private static final String CATE_SHIP_SYSTEM = "shipSystem";
    private static final String CATE_STAR_SYSTEMS = "starSystems";
    private static final String CATE_HULL_MOD = "hullMod";

    public static String getString(String category, String id) {
        return Global.getSettings().getString(category, id);
    }

    public static String getShipSystemString(String id) {
        return getString(CATE_SHIP_SYSTEM, id);
    }

    public static String getStarSystemsString(String id) {
        return getString(CATE_STAR_SYSTEMS, id);
    }

    public static String getHullModString(String id) {
        return getString(CATE_HULL_MOD, id);
    }

    //useful things by Nitori_Tachyon
    public static void outputStringSetToDialog(TextPanelAPI text, String category, String stringSet, Color color)
    {
        outputStringSetToDialog(text,category,stringSet,0,color);
    }
    public static void outputStringSetToDialog(TextPanelAPI text,String category,String stringSet, int start, Color color)
    {
        for (int i=start;!(getString(category,stringSet+"_"+i).contains("Missing string"));i+=1)
        {
            boolean characterColored=getString(category,stringSet+"_"+i).charAt(0)=='*';
            String string=getString(category,stringSet+"_"+i);
            if (!characterColored)text.addPara(string);
            else text.addPara(string.substring(1),color);
        }
    }

}
