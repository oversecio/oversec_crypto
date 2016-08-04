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

package io.oversec.one.crypto.images;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


@SuppressLint("CommitPrefEdits")
public class ImagePreferences {

    private static final String PREF_IMAGE__CODER = "coder";


    private static ImagePreferences mImagePreferences;
    private SharedPreferences mSharedPreferences;

    private static String PREF_FILE_NAME = ImagePreferences.class.getSimpleName();


    public static synchronized ImagePreferences getPreferences(Context context) {
        if (mImagePreferences == null) {
            mImagePreferences = new ImagePreferences(context);
        }
        return mImagePreferences;
    }


    private ImagePreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, 0);
    }


    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }

    public void setXCoder(String name) {
        mSharedPreferences.edit().putString(PREF_IMAGE__CODER, name).commit();
    }


    public String getXCoder() {
        return mSharedPreferences.getString(PREF_IMAGE__CODER, null);
    }

}
