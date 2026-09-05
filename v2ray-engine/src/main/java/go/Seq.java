package go;

import android.content.Context;

public class Seq {
    private static Context context;

    public static void setContext(Context ctx) {
        context = ctx;
    }

    public static Context getContext() {
        return context;
    }
}