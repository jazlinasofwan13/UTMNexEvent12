package com.example.utmnexevent;

public class AvatarHelper {

    // These are the internal names for the database
    public static final String AVATAR_1 = "avatar_1";
    public static final String AVATAR_2 = "avatar_2";
    public static final String AVATAR_3 = "avatar_3";
    public static final String AVATAR_4 = "avatar_4";

    /**
     * Maps the avatar string from database to a real image in the drawable folder.
     */
    public static int getAvatarResource(String avatarId) {
        if (avatarId == null) return R.drawable.avatar_1; // Default

        switch (avatarId) {
            case AVATAR_1: return R.drawable.avatar_1;
            case AVATAR_2: return R.drawable.avatar_2;
            case AVATAR_3: return R.drawable.avatar_3;
            case AVATAR_4: return R.drawable.avatar_4;
            default: return R.drawable.avatar_1;
        }
    }
}
