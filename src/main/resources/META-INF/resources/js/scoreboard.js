const Scoreboard = (function () {

    class ScoreboardClass {
        constructor(scene,
                    players,
                    x,
                    y) {
            this.scene = scene;
            this.players = players;
            this.x = x;
            this.y = y;

            this.entries = [];
            this.alpha = 1;
            this.depth = 1;

        }

        setupEventQueue(eventQueue) {
            eventQueue.add('SCOREBOARD_UPDATED', resp => {
                this.process(resp);
            });
            return this;
        }

        setAlpha(alpha) {
            this.alpha = alpha;
            this.entries.forEach(entry => entry.setAlpha(alpha));
            return this;
        }

        setDepth(depth) {
            this.depth = depth;
            this.entries.forEach(entry => entry.setDepth(depth));
            return this;
        }

        process(scoreboardUpdatedEvent) {
            scoreboardUpdatedEvent.scores.forEach((entry, i) => {
                if (i >= this.entries.length) {
                    let y = 0;
                    if (i > 0) {
                        y = this.entries[0].height;
                    }
                    const text = this.scene.add.bitmapText(this.x + 5, this.y + i * y + 4, 'font', '', 16);

                    text.setScrollFactor(0);
                    text.setAlpha(this.alpha);
                    text.setDepth(this.depth);
                    this.entries.push(text);
                }
                this.entries[i].setText(entry.playerId.substr(0, 7) + ": " + entry.score);
                if (this.players.get(entry.playerId)) {
                    this.entries[i].setTintFill(this.players.get(entry.playerId).ship.color);
                } else {
                    this.entries[i].setTintFill(0xffffff);
                }
            });
        }
    }

    let instance = 0;

    return {
        'get': (scene,
                players,
                x,
                y) => {
            if (!instance) {
                instance = new ScoreboardClass(scene, players, x, y)
            }
            return instance;
        }
    }
})();