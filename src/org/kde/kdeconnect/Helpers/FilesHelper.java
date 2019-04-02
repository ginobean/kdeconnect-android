/*
 * Copyright 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License or (at your option) version 3 or any later version
 * accepted by the membership of KDE e.V. (or its successor approved
 * by the membership of KDE e.V.), which shall act as a proxy
 * defined in Section 14 of version 3 of the license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

package org.kde.kdeconnect.Helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.kde.kdeconnect.NetworkPacket;

import java.io.File;
import java.io.InputStream;

public class FilesHelper {

    private static String getFileExt(String filename) {
        //return MimeTypeMap.getFileExtensionFromUrl(filename);
        return filename.substring((filename.lastIndexOf(".") + 1));
    }

    public static String getFileNameWithoutExt(String filename) {
        int dot = filename.lastIndexOf(".");
        return (dot < 0) ? filename : filename.substring(0, dot);
    }

    public static String getMimeTypeFromFile(String file) {
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExt(file));
        if (mime == null) mime = "*/*";
        return mime;
    }

    public static String findNonExistingNameForNewFile(String path, String filename) {
        int dot = filename.lastIndexOf(".");
        String name = (dot < 0) ? filename : filename.substring(0, dot);
        String ext = (dot < 0) ? "" : filename.substring(filename.lastIndexOf("."));

        int num = 1;
        while (new File(path + "/" + filename).exists()) {
            filename = name + " (" + num + ")" + ext;
            num++;
        }

        return filename;
    }

    //Following code from http://activemq.apache.org/maven/5.7.0/kahadb/apidocs/src-html/org/apache/kahadb/util/IOHelper.html

    /**
     * Converts any string into a string that is safe to use as a file name.
     * The result will only include ascii characters and numbers, and the "-","_", and "." characters.
     */
    private static String toFileSystemSafeName(String name, boolean dirSeparators, int maxFileLength) {
        int size = name.length();
        StringBuilder rc = new StringBuilder(size * 2);
        for (int i = 0; i < size; i++) {
            char c = name.charAt(i);
            boolean valid = c >= 'a' && c <= 'z';
            valid = valid || (c >= 'A' && c <= 'Z');
            valid = valid || (c >= '0' && c <= '9');
            valid = valid || (c == '_') || (c == '-') || (c == '.');
            valid = valid || (dirSeparators && ((c == '/') || (c == '\\')));

            if (valid) {
                rc.append(c);
            }
        }
        String result = rc.toString();
        if (result.length() > maxFileLength) {
            result = result.substring(result.length() - maxFileLength);
        }
        return result;
    }

    public static String toFileSystemSafeName(String name, boolean dirSeparators) {
        return toFileSystemSafeName(name, dirSeparators, 255);
    }

    public static String toFileSystemSafeName(String name) {
        return toFileSystemSafeName(name, true, 255);
    }

    private static int GetOpenFileCount() {
        return new File("/proc/self/fd").listFiles().length;
    }

    public static void LogOpenFileCount() {
        Log.e("KDE/FileCount", "" + GetOpenFileCount());
    }


    //Create the network package from the URI
    public static NetworkPacket uriToNetworkPacket(final Context context, final Uri uri, String type) {

        try {

            ContentResolver cr = context.getContentResolver();
            InputStream inputStream = cr.openInputStream(uri);

            NetworkPacket np = new NetworkPacket(type);
            long size = -1;

            if (uri.getScheme().equals("file")) {
                // file:// is a non media uri, so we cannot query the ContentProvider

                np.set("filename", uri.getLastPathSegment());

                try {
                    size = new File(uri.getPath()).length();
                } catch (Exception e) {
                    Log.e("SendFileActivity", "Could not obtain file size", e);
                }

            } else {
                // Probably a content:// uri, so we query the Media content provider
                String[] proj = {
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                };

                try (Cursor cursor = cr.query(uri, proj, null, null, null)) {
                    int nameColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                    int sizeColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                    cursor.moveToFirst();

                    String filename = cursor.getString(nameColumnIndex);
                    size = cursor.getInt(sizeColumnIndex);

                    np.set("filename", filename);
                } catch (Exception e) {
                    Log.e("SendFileActivity", "Problem getting file information", e);
                }
            }

            np.setPayload(new NetworkPacket.Payload(inputStream, size));

            return np;
        } catch (Exception e) {
            Log.e("SendFileActivity", "Exception creating network packet", e);
            return null;
        }
    }
}
