package edu.rice.starvote.ballotbox.swingui;

import javax.sound.sampled.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Created by cyricc on 9/20/2016.
 */
public class VoicePlayer {

    private final Map<String, Clip> clipCache = new ConcurrentHashMap<>(2);
    private final Semaphore lock = new Semaphore(1);

    /**
     * Plays the specified WAV audio file. This method returns immediately after playback begins. If this player is
     * currently playing another file, playback of the new file will not begin until the previous file is finished
     * playing.
     *
     * If the caller desires to block until playback is complete, use `waitUntilFinished()` after invoking this method.
     *
     * @param path Path to audio file. Must be on the resource path.
     * @throws IOException If an I/O error occurs, or file could not be found.
     * @throws UnsupportedAudioFileException If the audio file is in an unsupported format.
     */
    public void play(String path) throws IOException, UnsupportedAudioFileException {
        if (clipCache.containsKey(path)) {
            try {
                lock.acquire();
                final Clip cachedClip = clipCache.get(path);
                cachedClip.setFramePosition(0);
                cachedClip.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            final InputStream fileStream = getClass().getClassLoader().getResourceAsStream(path);
            if (fileStream == null) throw new FileNotFoundException("File " + path + " could not be found.");
            final AudioInputStream audioStream = AudioSystem.getAudioInputStream(fileStream);
            final DataLine.Info info = new DataLine.Info(Clip.class, audioStream.getFormat());
            try {
                final Clip clip = (Clip) AudioSystem.getLine(info);
                clip.addLineListener((e) -> {
                    final LineEvent.Type type = e.getType();
                    if (type.equals(LineEvent.Type.START)) {
                        assert lock.availablePermits() == 1 || lock.availablePermits() == 0 : "Synchronization error; multiple semaphores generated";
                    } else if (type.equals(LineEvent.Type.STOP)) {
                        lock.release();
                        System.out.println("stopped " + lock.availablePermits());
                    }
                });
                lock.acquire();
                clip.open(audioStream);
                clip.start();
                clipCache.put(path, clip);
            } catch (LineUnavailableException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method blocks until the current audio file is finished playing. If no audio is playing, returns
     * immediately.
     */
    public void waitUntilFinished() {
        try {
            System.out.println("attempting " + lock.availablePermits());
            lock.acquire();
            System.out.println("acquired " + lock.availablePermits());
            lock.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
