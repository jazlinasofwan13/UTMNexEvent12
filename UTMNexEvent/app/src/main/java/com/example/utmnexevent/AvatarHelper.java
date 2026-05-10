package com.example.utmnexevent;

import android.widget.ImageView;

public class AvatarHelper {

    // These are the internal names for the database
    public static final String AVATAR_1 = "avatar_1";
    public static final String AVATAR_2 = "avatar_2";
    public static final String AVATAR_3 = "avatar_3";
    public static final String AVATAR_4 = "avatar_4";

    /**
     * Maps the avatar string from database to a real image in the drawable folder.
     * You can replace these drawables with your own images later.
     */
    public static int getAvatarResource(String avatarId) {
        if (avatarId == null) return android.R.drawable.ic_menu_gallery; // Default

        switch (avatarId) {
            case AVATAR_1: return android.R.drawable.ic_menu_camera;
            case AVATAR_2: return android.R.drawable.ic_menu_gallery;
            case AVATAR_3: return android.R.drawable.ic_menu_slideshow;
            case AVATAR_4: return android.R.drawable.ic_menu_compass;
            default: return android.R.drawable.ic_menu_gallery;
        }
    }
}
