const Hud = (function () {

    /**
     * Message that appears in the center screen
     */
    class CenterMessageHudClass {
        constructor(scene, gameConfig) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.centerMessage = null;
        }


        show(message, color, showIfFunction) {
            if (!this.centerMessage) {
                this.centerMessage = [];
                this.centerMessage.push(this.scene.add.bitmapText(-100, -100, 'font', message, this.gameConfig.hud.fontSize.center));
                this.centerMessage.push(this.scene.add.bitmapText(-100, -100, 'font', message, this.gameConfig.hud.fontSize.center));

                this.centerMessage.forEach(text => {
                    text.setScrollFactor(0);
                });

                this.centerMessage[0]
                    .setAlpha(this.gameConfig.hud.alpha)
                    .setDepth(this.gameConfig.hud.depth);

                this.centerMessage[1]
                    .setAlpha(1)
                    .setDepth(this.gameConfig.background.effectsDepth);


            }

            this.centerMessage.forEach(t => {
                t
                    .setTint(color)
                    .setText(message);

                t.x = (this.scene.game.config.width / 2) - (t.width / 2);
                t.y = (this.scene.game.config.height / 2) - (t.height);
            });

            if (!!showIfFunction) {
                setTimeout(() => this.centerMessage.forEach(t =>
                    t
                        .setTint(color)
                        .setActive(true)
                        .setVisible(true)
                ), 700);
            } else {
                this.centerMessage.forEach(t =>
                    t
                        .setTint(color)
                        .setActive(true)
                        .setVisible(true)
                );
            }
        }

        hide() {
            if (this.centerMessage) {
                this.centerMessage.forEach(t =>
                    t
                        .setActive(false)
                        .setVisible(false)
                );
            }
        }
    }

    /**
     * Scoreboard shows 10 leaders on the top left
     */
    class ScoreBoardHudClass {

        constructor(scene, gameConfig, colors) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.colors = colors;

            this.entries = [];
            this.getPlayer = () => null;
        }

        update(entries) {
            entries.forEach((entry, i) => {
                if (i >= this.entries.length) {
                    let y = 0;
                    if (i > 0) {
                        y = this.entries[0].height;
                    }
                    const text = this.scene.add.bitmapText(5, i * y + 4, 'font', '', this.gameConfig.hud.fontSize.periphery);

                    text.setScrollFactor(0);
                    text.setAlpha(this.gameConfig.hud.alpha);
                    text.setDepth(this.gameConfig.hud.depth);
                    this.entries.push(text);
                }
                const p = this.getPlayer(entry.playerId.guid);
                if (p) {
                    let n = p.name;
                    while(n.length < this.gameConfig.hud.nameLength) {
                        n += ' ';
                    }
                    this.entries[i].setText(n + " " + entry.score);
                    this.entries[i].setTintFill(p.ship.color);
                } else {
                    this.entries[i].setText(entry.playerId.guid.substr(0, this.gameConfig.hud.nameLength) + " " + entry.score);
                    this.entries[i].setTintFill(this.colors.getDarkTextColor().toRgbNumber());
                }
            });
        }

        setGetPlayer(getPlayer) {
            this.getPlayer = getPlayer;
        }
    }

    /**
     * Show's player's score on top right
     */
    class PlayerScoreHudClass {
        constructor(scene, gameConfig) {
            this.scene = scene;
            this.gameConfig = gameConfig;

            this.scoreView = null;
        }

        update(resp, displayName, color) {
            if (!this.scoreView) {
                const text = this.scene.add.bitmapText(0, 4, 'font', '', this.gameConfig.hud.fontSize.periphery);
                text.setScrollFactor(0);
                text.setAlpha(this.gameConfig.hud.alpha);
                text.setDepth(this.gameConfig.hud.depth);
                text.setTintFill(color);

                this.scoreView = text;
            }

            this.scoreView.setText(displayName + ' ' + resp.score);
            this.scoreView.x = this.scene.game.config.width - (this.scoreView.width + 5);
        }
    }

    class LatestActionsHudClass {
        constructor(scene, gameConfig, colors) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.colors = colors;

            this.latestActions = new Array(4);
            this.latestActionsTexts = new Array(4);
            this.latestActionsWriteIndex = 0;
            this.latestActionsWrapMod = 3;
            this.latestActionsChanged = false;

            this.getPlayer = () => null;
        }

        append(action) {
            this.latestActions[this.latestActionsWriteIndex] = action;
            this.latestActionsWriteIndex = (this.latestActionsWriteIndex + 1) & this.latestActionsWrapMod;
            this.latestActionsChanged = true;
        }


        show() {
            if (this.latestActionsChanged) {
                let i = this.latestActionsWriteIndex;
                for (let c = 0; c < this.latestActions.length; c++) {
                    if (this.latestActions[i]) {
                        if (!this.latestActionsTexts[c]) {
                            this.latestActionsTexts[c] = [];
                            for (let p = 0; p < 4; p++) {
                                const text = this.scene.add.bitmapText(5, -100, 'font', 'X', this.gameConfig.hud.fontSize.periphery);
                                text.setScrollFactor(0);
                                text.setAlpha(this.gameConfig.hud.alpha);
                                text.setDepth(this.gameConfig.hud.depth);

                                const textPos = this.latestActions.length - c;
                                text.y = this.scene.game.config.height - (text.height * textPos + 5);
                                text
                                    .setTintFill(this.colors.getDarkTextColor().toRgbNumber())
                                    .setText('');
                                this.latestActionsTexts[c].push(text);
                            }
                        }

                        if (this.latestActions[i].playerDestroyed) {
                            const playerDestroyed = this.latestActions[i].playerDestroyed;

                            const destroyer = this.getPlayer(playerDestroyed.destroyedByPlayerId.guid);
                            const destroyerLabel = destroyer ?
                                destroyer.displayName :
                                playerDestroyed.destroyedByPlayerId.guid.substr(0, this.gameConfig.hud.nameLength);

                            const destroyed = this.getPlayer(playerDestroyed.playerId.guid);
                            const destroyedLabel = destroyed ?
                                destroyed.displayName :
                                playerDestroyed.playerId.guid.substr(0, this.gameConfig.hud.nameLength);

                            const timestamp = new Date(playerDestroyed.destroyedTimestamp).toTimeString().substr(0, 8);


                            let xoffset = this.latestActionsTexts[c][0]
                                .setText(timestamp + " ")
                                .width;

                            this.latestActionsTexts[c][1]
                                .setTintFill(destroyer ? destroyer.ship.color : this.colors.getDarkTextColor().toRgbNumber())
                                .setText(destroyerLabel)
                                .x = xoffset;

                            xoffset += this.latestActionsTexts[c][1].width;

                            this.latestActionsTexts[c][2]
                                .setText(" destroys ")
                                .x = xoffset;

                            xoffset += this.latestActionsTexts[c][2].width;

                            this.latestActionsTexts[c][3]
                                .setTintFill(destroyed ? destroyed.ship.color : this.colors.getDarkTextColor().toRgbNumber())
                                .setText(destroyedLabel)
                                .x = xoffset;
                        }
                    }
                    i = (i + 1) & this.latestActionsWrapMod;
                }
                this.latestActionsChanged = false;
            }
        }

        setGetPlayer(getPlayer) {
            this.getPlayer = getPlayer;
        }

        start() {
            setInterval(() => this.show(), 500);
            return this;
        }
    }

    class HudClass {

        constructor(scene, gameConfig, colors) {
            this.centerMessage = new CenterMessageHudClass(scene, gameConfig);
            this.scoreBoard = new ScoreBoardHudClass(scene, gameConfig, colors);
            this.playerScore = new PlayerScoreHudClass(scene, gameConfig);
            this.latestActions = new LatestActionsHudClass(scene, gameConfig, colors);
        }

        setGetPlayer(getPlayer) {
            this.scoreBoard.setGetPlayer(getPlayer);
            this.latestActions.setGetPlayer(getPlayer);
        }

        start() {
            this.latestActions.start();
            return this;
        }

    }

    let instance = null;

    return {
        'get': (scene, gameConfig, colors) => {
            if (!instance) {
                instance = new HudClass(scene, gameConfig, colors)
                    .start();
            }
            return instance;
        }
    }

})();