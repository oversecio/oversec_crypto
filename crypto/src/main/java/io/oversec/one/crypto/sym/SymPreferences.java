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

package io.oversec.one.crypto.sym;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


@SuppressLint("CommitPrefEdits")
public class SymPreferences {

    private static final String PREF_KEY_SYM_TTL = "keystore_ttl_sym";
    private static final String PREF_KEY_SIMPLE_TTL = "keystore_ttl_simple";
    private static SymPreferences mSymPreferences;
    private SharedPreferences mSharedPreferences;

    private static String PREF_FILE_NAME = SymPreferences.class.getSimpleName();


    public static synchronized SymPreferences getPreferences(Context context) {
        if (mSymPreferences == null) {
            mSymPreferences = new SymPreferences(context);
        }
        return mSymPreferences;
    }


    private SymPreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, 0);
    }


    public int getKeystoreSymTTL() {
        return mSharedPreferences.getInt(PREF_KEY_SYM_TTL, 0);
    }

    public void setKeystoreSymTTL(int v) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_KEY_SYM_TTL, v);
        editor.commit();
    }


    public int getKeystoreSimpleTTL() {
        return mSharedPreferences.getInt(PREF_KEY_SIMPLE_TTL, Integer.MAX_VALUE);
    }

    public void setKeystoreSimpleTTL(int v) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt(PREF_KEY_SIMPLE_TTL, v);
        editor.commit();
    }

    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }

}
