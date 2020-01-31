const Scoreboard = (function () {

    class ScoreboardClass {
        constructor(gameConfig,
                    scene,
                    players,
                    x,
                    y) {
            this.gameConfig = gameConfig;
            this.scene = scene;
            this.players = players;
            this.x = x;
            this.y = y;

            this.entries = [];
            this.alpha = 1;
            this.depth = 1;

        }

        setupEventQueue(eventQueue) {
            eventQueue.add('scoreBoardUpdated', resp => {
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
                const p = this.players.get(entry.playerId.guid);
                if (p) {
                    let n = p.name;
                    while(n.length < this.gameConfig.hud.nameLength) {
                        n += ' ';
                    }
                    this.entries[i].setText(n + " " + entry.score);
                    this.entries[i].setTintFill(p.ship.color);
                } else {
                    this.entries[i].setText(entry.playerId.guid.substr(0, this.gameConfig.hud.nameLength) + " " + entry.score);
                    this.entries[i].setTintFill(0xffffff);
                }
            });
        }
    }

    let instance = 0;

    return {
        'get': (gameConfig,
                scene,
                players,
                x,
                y) => {
            if (!instance) {
                instance = new ScoreboardClass(gameConfig, scene, players, x, y)
            }
            return instance;
        }
    }
})();

try {
    module.exports = Scoreboard;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}