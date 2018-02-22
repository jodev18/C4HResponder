package dev.jojo.c4hresponder.core;

import android.app.Application;

import dev.jojo.c4hresponder.R;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by MAC on 18/02/2018.
 */

public class C4HApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/helvetica.otf")
                .setFontAttrId(R.attr.font)
                .build()
        );
    }
}
