const Scoreboard = (function () {

    class ScoreboardClass {
        constructor(hud,
                    players) {
            this.hud = hud;
            this.players = players;

        }

        setupEventQueue(eventQueue) {
            eventQueue.add('scoreBoardUpdated', resp => {
                this.process(resp);
            });
            return this;
        }

        process(scoreboardUpdatedEvent) {
            this.hud.scoreBoard.update(scoreboardUpdatedEvent.scores);
        }
    }

    let instance = 0;

    return {
        'get': (hud,
                players) => {
            if (!instance) {
                instance = new ScoreboardClass(hud, players)
            }
            return instance;
        }
    }
})();
