package uk.co.cpsd.javaproject1;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

public class AudioPlayerFactory {

    public static AudioPlayer createAudioPlayer() {

        if (isAudioSyatemAvailable()) {
            return new SoundPlayer();
        } else {
            return new NoOpAudioPlayer();
        }

    }

    public static boolean isAudioSyatemAvailable() {

        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            return mixers.length > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
