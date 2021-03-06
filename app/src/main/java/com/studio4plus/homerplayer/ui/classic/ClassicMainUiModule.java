package com.studio4plus.homerplayer.ui.classic;

import android.app.Activity;

import com.studio4plus.homerplayer.ui.MainActivity;
import com.studio4plus.homerplayer.ui.MainUi;
import com.studio4plus.homerplayer.ui.ActivityScope;
import com.studio4plus.homerplayer.ui.SpeakerProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class ClassicMainUiModule {
    private final MainActivity activity;

    public ClassicMainUiModule(MainActivity activity) {
        this.activity = activity;
    }

    @Provides @ActivityScope
    MainUi mainUi(Activity activity) {
        return new ClassicMainUi(activity);
    }

    @Provides @ActivityScope
    Activity activity() {
        return activity;
    }

    @Provides @ActivityScope
    SpeakerProvider speakProvider() {
        return activity;
    }
}
