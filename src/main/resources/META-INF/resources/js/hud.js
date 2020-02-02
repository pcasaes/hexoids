const Hud = (function () {

    function toFixedWithName(name, length, char) {
        if (!char) {
            char = ' ';
        }
        while (name.length < length) {
            name += char;
        }
        return name;
    }

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
                this.centerMessage.push(this.scene.add.bitmapText(-100, -100, 'font', message, this.gameConfig.hud.font.size.center));
                this.centerMessage.push(this.scene.add.bitmapText(-100, -100, 'font', message, this.gameConfig.hud.font.size.center));

                this.centerMessage.forEach(text => {
                    text.setScrollFactor(0);
                    text.scaleX = this.gameConfig.hud.font.scale.width;
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
                    const text = this.scene.add.bitmapText(
                        this.gameConfig.hud.font.offset.x,
                        i * y + this.gameConfig.hud.font.offset.y,
                        'font',
                        '',
                        this.gameConfig.hud.font.size.periphery);
                    text.scaleX = this.gameConfig.hud.font.scale.width;

                    text.setScrollFactor(0);
                    text.setAlpha(this.gameConfig.hud.alpha);
                    text.setDepth(this.gameConfig.hud.depth);
                    this.entries.push(text);
                }
                const p = this.getPlayer(entry.playerId.guid);
                if (p) {
                    this.entries[i].setText(p.displayName + " " + entry.score);
                    this.entries[i].setTintFill(p.color);
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
        constructor(scene, gameConfig, colors) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.colors = colors;

            this.scoreView = null;
        }

        update(resp, p) {
            if (!this.scoreView) {
                const text = this.scene.add.bitmapText(
                    this.gameConfig.hud.font.offset.x,
                    this.gameConfig.hud.font.offset.y,
                    'font',
                    '',
                    this.gameConfig.hud.font.size.periphery);
                text.setScrollFactor(0);
                text.setAlpha(this.gameConfig.hud.alpha);
                text.setDepth(this.gameConfig.hud.depth);
                text.setTintFill(p ? p.color : this.colors.getDarkTextColor());
                text.scaleX = this.gameConfig.hud.font.scale.width;

                this.scoreView = text;
            }
            const name = p ? p.displayName : resp.playerId.guid.substr(0, this.gameConfig.hud.nameLength);

            this.scoreView.setText(name + ' ' + resp.score);
            this.scoreView.x = this.scene.game.config.width - (this.scoreView.width - this.gameConfig.hud.font.offset.x);
        }
    }

    class NearbyPlayersHudClass {
        constructor(scene, gameConfig, colors, transform) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.colors = colors;
            this.transform = transform;

            this.getPlayer = () => null;
            this.myPlayerId = null;
            this.texts = new Array(10);
            this.playerIdIndex = {};
            this.playerInView = 0;
        }

        setGetPlayer(getPlayer) {
            this.getPlayer = getPlayer;
        }


        setMyPlayerId(id) {
            this.myPlayerId = id;
        }


        removeAll() {
            Object.keys(this.playerIdIndex).forEach((playerId) => {
                this.remove(playerId);
            });
        }

        remove(playerId) {
            if (!this.contains(playerId)) {
                return;
            }

            const text = this.texts[this.playerIdIndex[playerId]];
            if (text && text.visible) {
                text
                    .setVisible(false)
                    .setActive(false);

                delete this.playerIdIndex[playerId];
                this.playerInView--;
            }
        }

        contains(playerId) {
            return this.playerIdIndex[playerId] || this.playerIdIndex[playerId] === 0;
        }

        update(resp) {
            if (resp.playerId.guid === this.myPlayerId) {
                return;
            }

            const move = this.transform.view(resp.x, resp.y);
            const playerId = resp.playerId.guid;
            if (this.transform.inView(move.x, move.y, this.scene.cameras.main.worldView)) {
                if (this.playerInView >= 10 || this.contains(playerId)) {
                    return;
                }

                const p = this.getPlayer(playerId);

                for (let i = 0; i < 10; i++) {
                    let text;

                    if (!this.texts[i]) {
                        text = this.scene.add.bitmapText(
                            this.gameConfig.hud.font.offset.x,
                            -100,
                            'font',
                            'Y',
                            this.gameConfig.hud.font.size.periphery);

                        text.setScrollFactor(0);
                        text.setAlpha(this.gameConfig.hud.alpha);
                        text.setDepth(this.gameConfig.hud.depth);
                        text.scaleX = this.gameConfig.hud.font.scale.width;
                        text.y = this.scene.game.config.height - (text.height * (i + 1) + 100);
                        this.texts[i] = text;
                    } else if (!this.texts[i].visible) {
                        text = this.texts[i]
                            .setVisible(true)
                            .setActive(true);
                    }

                    if (text) {
                        text.setTintFill(p ? p.color : this.colors.getDarkTextColor());
                        text.setText(p ? p.displayName : playerId.sub(0, this.gameConfig.hud.nameLength));

                        text.x = this.scene.game.config.width - (text.width - this.gameConfig.hud.font.offset.x);

                        this.playerIdIndex[playerId] = i;
                        this.playerInView++;
                        break;
                    }
                }
            } else {
                this.remove(playerId);
            }
        }
    }

    /**
     * Show's latest in game action on the bottom left.
     */
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
                                const text = this.scene.add.bitmapText(
                                    this.gameConfig.hud.font.offset.x,
                                    -100,
                                    'font',
                                    'X',
                                    this.gameConfig.hud.font.size.periphery);
                                text.setScrollFactor(0);
                                text.setAlpha(this.gameConfig.hud.alpha);
                                text.setDepth(this.gameConfig.hud.depth);
                                text.scaleX = this.gameConfig.hud.font.scale.width;

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
                                destroyer.actionName :
                                playerDestroyed.destroyedByPlayerId.guid.substr(0, this.gameConfig.hud.nameLength);

                            const destroyed = this.getPlayer(playerDestroyed.playerId.guid);
                            const destroyedLabel = destroyed ?
                                destroyed.name :
                                playerDestroyed.playerId.guid.substr(0, this.gameConfig.hud.nameLength);

                            const timestamp = new Date(playerDestroyed.destroyedTimestamp);
                            const timeStr = `${timestamp.getUTCHours() < 10 ? '0' : ''}${timestamp.getUTCHours()}${timestamp.getUTCMinutes() < 10 ? '0' : ''}${timestamp.getUTCMinutes()}${timestamp.getUTCSeconds() < 10 ? '0' : ''}${timestamp.getUTCSeconds()}`;

                            let xOffset = this.latestActionsTexts[c][0]
                                .setText(timeStr + " ")
                                .width;

                            this.latestActionsTexts[c][1]
                                .setTintFill(destroyer ? destroyer.color : this.colors.getDarkTextColor().toRgbNumber())
                                .setText(destroyerLabel)
                                .x = xOffset;

                            xOffset += this.latestActionsTexts[c][1].width;

                            this.latestActionsTexts[c][2]
                                .setText("-*")
                                .setTintFill(destroyer ? destroyer.color : this.colors.getDarkTextColor().toRgbNumber())
                                .x = xOffset;

                            xOffset += this.latestActionsTexts[c][2].width;

                            this.latestActionsTexts[c][3]
                                .setTintFill(destroyed ? destroyed.color : this.colors.getDarkTextColor().toRgbNumber())
                                .setText(destroyedLabel)
                                .x = xOffset;
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

        constructor(scene, gameConfig, colors, transform) {
            this.gameConfig = gameConfig;
            this.colors = colors;
            this.centerMessage = new CenterMessageHudClass(scene, gameConfig);
            this.scoreBoard = new ScoreBoardHudClass(scene, gameConfig, colors);
            this.playerScore = new PlayerScoreHudClass(scene, gameConfig, colors);
            this.latestActions = new LatestActionsHudClass(scene, gameConfig, colors);
            this.nearestPlayers = new NearbyPlayersHudClass(scene, gameConfig, colors, transform);
            this.players = {};
            this.myPlayerId = null;
        }

        setMyPlayerId(id) {
            this.myPlayerId = id;
            this.nearestPlayers.setMyPlayerId(id);
        }

        setupQueues(queues) {
            const addPlayer = (resp) => {
                this.players[resp.playerId.guid] = {
                    'name': resp.name,
                    'displayName': toFixedWithName(resp.name, this.gameConfig.hud.nameLength),
                    'actionName': toFixedWithName(resp.name, this.gameConfig.hud.nameLength, '-'),
                    'color': this.colors.get(resp.ship).toRgbNumber()
                };
            };

            queues.command
                .add('playersList', (resp, dto) => {
                    this.setMyPlayerId(dto.directedCommand.playerId.guid);
                    resp.players.forEach(r => addPlayer(r));
                })
                .add('playerScoreUpdate', (resp, dto) => {
                    this.playerScore.update(resp, this.players[dto.directedCommand.playerId.guid])
                });


            queues.event
                .add('playerJoined', resp => addPlayer(resp))
                .add('playerLeft', resp => {
                    delete this.players[resp.playerId.guid];
                    this.nearestPlayers.remove(resp.playerId.guid);
                })
                .add('DISCONNECTED', resp => {
                    this.players = {};
                    this.nearestPlayers.removeAll();
                })
                .add('playerMoved', resp => {
                    this.nearestPlayers.update(resp);
                })
                .add('playerSpawned', resp => {
                    resp = resp.location;
                    this.nearestPlayers.update(resp);
                    if (this.myPlayerId === resp.playerId.guid) {
                        this.nearestPlayers.removeAll();
                    }
                })
                .add('playerDestroyed', resp => {
                    this.nearestPlayers.remove(resp.playerId.guid);
                });

            return this;
        }

        start() {
            const getPlayer = (id) => {
                return this.players[id];
            };

            this.scoreBoard.setGetPlayer(getPlayer);
            this.latestActions.setGetPlayer(getPlayer);
            this.nearestPlayers.setGetPlayer(getPlayer);

            this.latestActions.start();
            return this;
        }

    }

    let instance = null;

    return {
        'get': (scene, gameConfig, colors, transform, queues) => {
            if (!instance) {
                instance = new HudClass(scene, gameConfig, colors, transform)
                    .setupQueues(queues)
                    .start();
            }
            return instance;
        }
    }

})();