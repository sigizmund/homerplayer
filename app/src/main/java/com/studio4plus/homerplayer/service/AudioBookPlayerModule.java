package com.studio4plus.homerplayer.service;

import android.content.Context;

import com.studio4plus.homerplayer.player.Player;

import dagger.Module;
import dagger.Provides;

@Module
public class AudioBookPlayerModule {
    @Provides
    Player provideAudioBookPlayer(Context context) {
        return new Player(context);
    }
}
