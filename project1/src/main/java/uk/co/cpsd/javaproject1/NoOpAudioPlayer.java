package uk.co.cpsd.javaproject1;

public class NoOpAudioPlayer implements AudioPlayer {

    @Override
    public void playSound(String resourcePath) {

        System.out.println("Audio not supported - would play: " + resourcePath);
    }

    @Override
    public boolean isAudioSupported() {
        return false;
    }
}
