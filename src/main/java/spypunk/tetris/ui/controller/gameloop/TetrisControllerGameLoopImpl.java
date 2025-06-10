/*
 * Copyright © 2016-2017 spypunk <spypunk@gmail.com>
 *
 * This work is free. You can redistribute it and/or modify it under the
 * terms of the Do What The Fuck You Want To Public License, Version 2,
 * as published by Sam Hocevar. See the COPYING file for more details.
 */

package spypunk.tetris.ui.controller.gameloop;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spypunk.tetris.guice.TetrisModule.TetrisProvider;
import spypunk.tetris.model.Tetris;
import spypunk.tetris.model.Tetris.State;
import spypunk.tetris.service.TetrisService;
import spypunk.tetris.sound.Sound;
import spypunk.tetris.sound.service.SoundService;
import spypunk.tetris.ui.controller.event.TetrisControllerTetrisEventHandler;
import spypunk.tetris.ui.controller.input.TetrisControllerInputHandler;
import spypunk.tetris.ui.view.TetrisMainView;

@Singleton
public final class TetrisControllerGameLoopImpl implements TetrisControllerGameLoop, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TetrisControllerGameLoopImpl.class);

    private static final int TICKS_PER_SECOND = 60;
    private static final int SKIP_TICKS = 1000 / TICKS_PER_SECOND;

    private final ExecutorService executorService;
    private final TetrisControllerInputHandler tetrisControllerInputHandler;
    private final TetrisControllerTetrisEventHandler tetrisControllerTetrisEventHandler;
    private final TetrisService tetrisService;
    private final TetrisMainView tetrisMainView;
    private final SoundService soundService; // -> 1. DEPENDÊNCIA ADICIONADA
    private final Tetris tetris;             // -> 1. DEPENDÊNCIA ADICIONADA

    private volatile boolean running;

    // -> 2. VARIÁVEL DE CONTROLE DO TIMER ADICIONADA
    private long countdownTickStart = 0;

    @Inject
    public TetrisControllerGameLoopImpl(final TetrisService tetrisService,
            final TetrisControllerInputHandler tetrisControllerInputHandler,
            final TetrisControllerTetrisEventHandler tetrisControllerTetrisEventHandler,
            final TetrisMainView tetrisMainView,
            final SoundService soundService, // -> 1. DEPENDÊNCIA ADICIONADA
            @TetrisProvider final Tetris tetris) { // -> 1. DEPENDÊNCIA ADICIONADA

        this.tetrisService = tetrisService;
        this.tetrisControllerInputHandler = tetrisControllerInputHandler;
        this.tetrisControllerTetrisEventHandler = tetrisControllerTetrisEventHandler;
        this.tetrisMainView = tetrisMainView;
        this.soundService = soundService; // -> 1. DEPENDÊNCIA ADICIONADA
        this.tetris = tetris;             // -> 1. DEPENDÊNCIA ADICIONADA

        executorService = Executors
                .newSingleThreadExecutor(runnable -> new Thread(runnable, "TetrisControllerGameLoop"));
    }

    @Override
    public void start() {
        running = true;
        executorService.execute(this);
    }

    @Override
    public void stop() {
        running = false;
        executorService.shutdown();
    }

    @Override
    public void run() {
        tetrisMainView.show();

        while (running) {
            long currentTick = System.currentTimeMillis();

            update();

            for (final long nextTick = currentTick + SKIP_TICKS; currentTick < nextTick; currentTick = System
                    .currentTimeMillis()) {
                waitMore();
            }
        }

        tetrisMainView.hide();
    }

    // -> 3. MÉTODO UPDATE MODIFICADO
    private void update() {
        tetrisControllerInputHandler.handleInputs();
        tetrisControllerTetrisEventHandler.handleEvents();

        final State currentState = tetris.getState();

        if (State.RUNNING.equals(currentState)) {
            // A lógica normal do jogo só roda se o estado for RUNNING
            tetrisService.update();
        } else if (State.COUNTDOWN.equals(currentState)) {
            // Se estivermos em contagem, chamamos nossa nova lógica
            handleCountdown();
        }

        tetrisMainView.update();
    }
    
    // -> 4. NOSSO NOVO MÉTODO DE LÓGICA DO TIMER
    private void handleCountdown() {
        // Se acabamos de entrar no estado de contagem (nenhum bipe foi tocado)
        if (tetris.getBeepsPlayed() == 0) {
            soundService.playSound(Sound.SHAPE_LOCKED); // Toca o primeiro bipe
            tetris.setBeepsPlayed(1);
            countdownTickStart = System.currentTimeMillis(); // Marca o tempo do primeiro bipe
        }
    
        // Verifica se já passou 1 segundo desde o último bipe
        if (System.currentTimeMillis() - countdownTickStart > 1000) {
            int beepsPlayed = tetris.getBeepsPlayed();
    
            if (beepsPlayed < 3) {
                // Se ainda não foram 3 bipes, toca o próximo
                soundService.playSound(Sound.SHAPE_LOCKED);
                tetris.setBeepsPlayed(beepsPlayed + 1);
                countdownTickStart = System.currentTimeMillis(); // Marca o tempo do bipe atual
            } else {
                // Se os 3 bipes já foram tocados, a contagem acabou!
                tetris.setState(State.RUNNING); // O jogo começa!
                soundService.resumeMusic();     // A música volta a tocar
            }
        }
    }

    private void waitMore() {
        try {
            Thread.sleep(1);
        } catch (final InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
            stop();
        }
    }
}