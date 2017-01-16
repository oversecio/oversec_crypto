/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.oversec.one.crypto.ui.util;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


@SuppressLint("CommitPrefEdits")
public class GotItPreferences {

    private static final String PREF_GOTIT_PREFIX = "gotit_";

    private static GotItPreferences mPreferences;

    private SharedPreferences mSharedPreferences;

    private static String PREF_FILE_NAME = GotItPreferences.class.getSimpleName() + "1";


    public static synchronized GotItPreferences getPreferences(Context context) {
        if (mPreferences == null) {
            mPreferences = new GotItPreferences(context.getApplicationContext());
        }
        return mPreferences;
    }


    private GotItPreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, 0);
    }


    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }

    public boolean isTooltipConfirmed(String res) {
        if (res==null) {
            return  false;
        }
        return mSharedPreferences.getBoolean(PREF_GOTIT_PREFIX + res, false);
    }

    public void setTooltipConfirmed(String res) {
        if (res==null) {
            return;
        }
        mSharedPreferences.edit().putBoolean(PREF_GOTIT_PREFIX + res, true).commit();
    }
}
