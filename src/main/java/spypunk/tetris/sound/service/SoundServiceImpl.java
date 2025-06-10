// Caminho: spypunk/tetris/sound/service/SoundServiceImpl.java

package spypunk.tetris.sound.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import spypunk.tetris.sound.Sound;
import spypunk.tetris.sound.SoundClip;
import spypunk.tetris.sound.cache.SoundClipCache;

@Singleton
public class SoundServiceImpl implements SoundService {

    private final SoundClipCache soundClipCache;

    private SoundClip currentMusicSoundClip;

    // -> 1. NOSSA NOVA VARIÁVEL DE CONTROLE
    private boolean isMusicMuted;

    @Inject
    public SoundServiceImpl(final SoundClipCache soundClipCache) {
        this.soundClipCache = soundClipCache;
    }

    @Override
    public void playMusic(final Sound sound) {
        stopMusic();

        currentMusicSoundClip = soundClipCache.getSoundClip(sound);

        // -> 2. CONDIÇÃO ADICIONADA: SÓ TOCA A MÚSICA SE ELA NÃO ESTIVER MUTADA
        if (!isMusicMuted) {
            currentMusicSoundClip.play();
        }
    }

    @Override
    public void pauseMusic() {
        if (currentMusicSoundClip != null) {
            currentMusicSoundClip.pause();
        }
    }

    @Override
    public void resumeMusic() {
        // -> 3. CONDIÇÃO ADICIONADA: SÓ RETOMA A MÚSICA SE ELA NÃO ESTIVER MUTADA
        if (currentMusicSoundClip != null && !isMusicMuted) {
            currentMusicSoundClip.play();
        }
    }

    @Override
    public void stopMusic() {
        if (currentMusicSoundClip != null) {
            currentMusicSoundClip.stop();
            currentMusicSoundClip = null;
        }
    }

    @Override
    public void playSound(final Sound sound) {
        final SoundClip clip = soundClipCache.getSoundClip(sound);

        clip.stop();
        clip.play();
    }

    @Override
    public void setMuted(final boolean muted) {
        // O mute geral (tecla M) continua funcionando da mesma forma
        soundClipCache.getAllSoundClips().forEach(soundClip -> soundClip.setMuted(muted));
    }
    
    // -> 4. NOSSO NOVO MÉTODO COM A LÓGICA CORRETA
    @Override
    public void toggleMusicMute() {
        // Inverte o estado do mute da música
        this.isMusicMuted = !this.isMusicMuted;

        if (this.isMusicMuted) {
            // Se acabamos de mutar, paramos a música atual
            stopMusic();
        } else {
            // Se acabamos de desmutar, a música voltará a tocar
            // na próxima vez que uma ação do jogo chamar playMusic()
            // (ex: iniciar um novo jogo).
        }
    }

    @Override
    public void increaseVolume() {
        soundClipCache.getAllSoundClips().forEach(SoundClip::increaseVolume);
    }

    @Override
    public void decreaseVolume() {
        soundClipCache.getAllSoundClips().forEach(SoundClip::decreaseVolume);
    }
}