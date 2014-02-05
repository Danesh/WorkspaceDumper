package com.example.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends ActionBarActivity {

    private static final String LICENSE = "\n<!--\n" +
            "     Copyright (C) 2013-2014 The CyanogenMod Project\n" +
            "\n" +
            "     Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
            "     you may not use this file except in compliance with the License.\n" +
            "     You may obtain a copy of the License at\n" +
            "\n" +
            "          http://www.apache.org/licenses/LICENSE-2.0\n" +
            "\n" +
            "     Unless required by applicable law or agreed to in writing, software\n" +
            "     distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            "     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            "     See the License for the specific language governing permissions and\n" +
            "     limitations under the License.\n" +
            "-->\n";

    class Folder extends Item {
        String title;
        ArrayList<Favorite> children = new ArrayList<Favorite>();

        public String toString(int level) {
            StringBuilder builder = new StringBuilder();
            builder.append(getSpacing(level)).append("<folder").append("\n");
            if (!TextUtils.isEmpty(title)) {
                builder.append(getSpacing(level + 1)).append("launcher:title=\"").append(title).append("\"").append("\n");
            }
            dumpCommonContent(builder, level + 1);
            builder.append(">\n");
            for (Favorite child : children) {
                builder.append(child.toString(level + 2));
            }
            builder.append(getSpacing(level)).append("</folder>\n");
            return builder.toString();
        }
    }

    abstract class Item {
        String x,y;
        int id, container;
        String screen;

        public String getSpacing(int level) {
            String spacing = "";
            for (int i = 0; i < level; i++) {
                spacing+="    ";
            }
            return spacing;
        }

        public final void dumpCommonContent(StringBuilder builder, int level) {
            builder.append(getSpacing(level)).append("launcher:screen=\"").append(screen).append("\"").append("\n");
            builder.append(getSpacing(level)).append("launcher:x=\"").append(x).append("\"").append("\n");
            builder.append(getSpacing(level)).append("launcher:y=\"").append(y).append("\"");
        }

        public String toString(int level) {
            return null;
        }
    }

    class Favorite extends Item {
        String packageName;
        String className;
        boolean isInsideFolder;

        public String toString(int level) {
            StringBuilder builder = new StringBuilder();
            builder.append(getSpacing(level)).append("<favorite").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:packageName=\"").append(packageName).append("\"").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:className=\"").append(className).append("\"").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:container=\"").append(container).append("\"");
            if (!isInsideFolder) {
                builder.append("\n");
                dumpCommonContent(builder, level + 1);
            }
            builder.append("/>\n");
            return builder.toString();
        }
    }

    class Widget extends Favorite {
        String spanX, spanY;

        public String toString(int level) {
            StringBuilder builder = new StringBuilder();
            builder.append(getSpacing(level)).append("<appwidget").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:packageName=\"").append(packageName).append("\"").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:className=\"").append(className).append("\"").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:spanX=\"").append(spanX).append("\"").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:spanY=\"").append(spanY).append("\"").append("\n");
            builder.append(getSpacing(level + 1)).append("launcher:container=\"").append(container).append("\"").append("\n");
            dumpCommonContent(builder, level + 1);
            builder.append("/>\n");
            return builder.toString();
        }
    }

    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("config.xml", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // itemId -> Item
        HashMap<Integer, Item> workspaceItems = new HashMap<Integer, Item>();

        Cursor c = getContentResolver().query(Uri.parse("content://com.android.launcher3.settings/favorites"), null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String appWidgetProvider = c.getString(c.getColumnIndex(Favorites.APPWIDGET_PROVIDER));
                String intent = c.getString(c.getColumnIndex(Favorites.INTENT));

                int itemType = c.getInt(c.getColumnIndex(Favorites.ITEM_TYPE));

                Item workspaceItem;

                if (itemType == Favorites.ITEM_TYPE_SHORTCUT || itemType == Favorites.ITEM_TYPE_APPLICATION) {
                    workspaceItem = new Favorite();
                    try {
                        Intent parsedIntent = Intent.parseUri(intent, 0);
                        ((Favorite) workspaceItem).packageName = parsedIntent.getComponent().getPackageName();
                        ((Favorite) workspaceItem).className = parsedIntent.getComponent().getClassName();
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    };
                } else if (itemType == Favorites.ITEM_TYPE_FOLDER) {
                    workspaceItem = new Folder();
                    ((Folder) workspaceItem).title =  c.getString(c.getColumnIndex(Favorites.TITLE));
                } else if (itemType == Favorites.ITEM_TYPE_APPWIDGET) {
                    workspaceItem = new Widget();
                    ((Widget) workspaceItem).packageName = ComponentName.unflattenFromString(appWidgetProvider).getPackageName();
                    ((Widget) workspaceItem).className = ComponentName.unflattenFromString(appWidgetProvider).getClassName();
                    ((Widget) workspaceItem).spanX = c.getString(c.getColumnIndex(Favorites.SPANX));
                    ((Widget) workspaceItem).spanY = c.getString(c.getColumnIndex(Favorites.SPANY));
                } else {
                    continue;
                }

                workspaceItem.container = c.getInt(c.getColumnIndex(Favorites.CONTAINER));
                workspaceItem.screen = c.getString(c.getColumnIndex(Favorites.SCREEN));
                workspaceItem.x = c.getString(c.getColumnIndex(Favorites.CELLX));
                workspaceItem.y = c.getString(c.getColumnIndex(Favorites.CELLY));
                workspaceItem.id = c.getInt(c.getColumnIndex("_ID"));

                if (workspaceItem.container > 0) {
                    ((Favorite) workspaceItem).isInsideFolder = true;
                    Item folder = workspaceItems.get(workspaceItem.container);
                    if (folder == null) {
                        folder = new Folder();
                        folder.id = workspaceItem.container;
                        workspaceItems.put(folder.id, folder);
                    }
                    ((Folder) folder).children.add((Favorite) workspaceItem);
                } else if (itemType == Favorites.ITEM_TYPE_FOLDER) {
                    Item folder = workspaceItems.get(41);
                    if (folder != null) {
                        ((Folder) workspaceItem).children = (ArrayList<Favorite>) ((Folder) folder).children.clone();
                    }
                    workspaceItems.put(workspaceItem.id, workspaceItem);
                } else {
                    workspaceItems.put(workspaceItem.id, workspaceItem);
                }
            }

            ArrayList<Item> items = new ArrayList<Item>(workspaceItems.values());
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item o, Item o2) {
                    return o.screen.compareTo(o2.screen);
                }
            });

            StringBuilder builder = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            builder.append(LICENSE);
            builder.append("<favorites xmlns:launcher=\"http://schemas.android.com/apk/res/com.android.launcher3\">");
            builder.append("\n");
            for (Item item : items) {
                builder.append(item.toString(1));
            }
            builder.append("</favorites>");
            writeToFile(builder.toString());
        }
    }


}
