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

package io.oversec.one.crypto.gpg;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


@SuppressLint("CommitPrefEdits")
public class GpgPreferences {

    private static final String PREF_PGP_OWN_PUBLIC_KEY_ID = "pgp_own_public_key_id";


    private static GpgPreferences mGpgPreferences;
    private SharedPreferences mSharedPreferences;

    private static String PREF_FILE_NAME = GpgPreferences.class.getSimpleName();


    public static synchronized GpgPreferences getPreferences(Context context) {
        if (mGpgPreferences == null) {
            mGpgPreferences = new GpgPreferences(context);
        }
        return mGpgPreferences;
    }


    private GpgPreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, 0);
    }


    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }

    public void setGpgOwnPublicKeyId(long keyId) {
        mSharedPreferences.edit().putLong(PREF_PGP_OWN_PUBLIC_KEY_ID, keyId).commit();
    }


    public long getGpgOwnPublicKeyId() {
        return mSharedPreferences.getLong(PREF_PGP_OWN_PUBLIC_KEY_ID, 0);
    }


}
