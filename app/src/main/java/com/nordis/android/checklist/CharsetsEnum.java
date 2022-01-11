package com.nordis.android.checklist;

import java.util.ArrayList;

public enum CharsetsEnum {
    WINDOWS("Windows -1251"),
    ISO_8859_5("ISO 8859-5"),
    KOI8_R("KOI8 -R"),
    KOI8_U("KOI8 -U"),
    OEM_855("OEM 855"),
    OEM_866("OEM 866"),
    MACINTOSH("Macintosh");

    private String charsetKey;


    CharsetsEnum() {
    }

    CharsetsEnum(String key) {

        this.charsetKey = key;
    }

    public String getKey(){
        return charsetKey;
    }




}
