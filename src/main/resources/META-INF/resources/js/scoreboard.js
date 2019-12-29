class Scoreboard {
    constructor(scene, players, x, y) {
        this.scene = scene;
        this.x = x;
        this.y = y;
        this.players = players;
        this.entries = [];
        this.alpha = 1;
        this.depth = 1;

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
            if (this.players[entry.playerId]) {
                this.entries[i].setTintFill(this.players[entry.playerId].color);
            } else {
                this.entries[i].setTintFill(0xffffff);
            }
        });
    }
}