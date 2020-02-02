const Players = (function () {

    const EMPTY_OBJ = {};

    const FULL_CIRCLE_IN_RADIANS = 2 * Math.PI;

    const HALF_CIRCLE_IN_RADIANS = Math.PI;

    function setShipThrustAnim(sprite, toplay) {
        const p = sprite.anims.getProgress();
        if (Number.isNaN(p) || p > 0.9) {
            sprite.anims.play(toplay);
            sprite.anims.chain("ship-rest");
        }
    }

    function calculateAngleDistance(a, b) {
        const abDiff = a - b;

        const d = Math.abs(abDiff) % FULL_CIRCLE_IN_RADIANS;
        const r = d > HALF_CIRCLE_IN_RADIANS ? FULL_CIRCLE_IN_RADIANS - d : d;


        return (abDiff >= 0 && abDiff <= HALF_CIRCLE_IN_RADIANS) ||
        (abDiff <= -HALF_CIRCLE_IN_RADIANS && abDiff >= -FULL_CIRCLE_IN_RADIANS) ? r : -r;
    }

    const OUT_OF_VIEW_SHIPS = {};

    class Ship {
        constructor(scene, gameConfig, transform, colors) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.transform = transform;
            this.colors = colors;

            this.sprite = null;
            this.wake = null;
            this.fireBolt = null;
            this.explosion = null;
            this.color = null;
            this.deathSound = false;
            this.viewable = true;
            this.alive = false;
            this.followed = false;
            this.id = null;
        }

        create(p, sounds) {
            this.id = p.playerId.guid;
            sounds.get('explosion1').create(
                this.gameConfig.ship.sound.max,
                this.gameConfig.ship.sound.debounce,
                this.gameConfig.ship.sound.distanceThreshold
            );

            this.sprite = this.scene.physics.add.sprite(-100, -100, 'ship');
            const color = this.colors.get(p.ship).toRgbNumber();
            this.sprite.setTint(color | 0x555555, color, color | 0x555555, color);
            this.sprite.setBounce(0, 0);
            this.sprite.setScale(0.3);
            this.sprite.setDepth(this.gameConfig.ship.depth);
            this.sprite.setCollideWorldBounds(true);
            this.alive = p.spawned || false; // p.spawned might be undefined so we need this bit of js nonsense
            if (this.alive) {
                const move = this.transform.view(p.x, p.y);
                this.sprite.x = move.x;
                this.sprite.y = move.y;
                this.sprite.setRotation(p.angle);
            }
            this.viewable = !this.alive;
            this.setViewable(this.alive);


            this.sprite.anims.play("ship-rest");

            const wake = {
                _triedAt: -1,
                firedAt: -1,
                sprites: [
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                    this.scene.physics.add.sprite(-100, -100, 'thrust'),
                ],
                nextSprite: 0,

                destroy: () => {
                    wake.sprites.forEach(s => s.destroy());
                },

                generate: () => {
                    wake._triedAt = Date.now();
                    if (wake._triedAt - wake.firedAt > 32) {
                        wake.firedAt = wake._triedAt;
                        wake.sprites[wake.nextSprite].x = this.sprite.x;
                        wake.sprites[wake.nextSprite].y = this.sprite.y;
                        wake.sprites[wake.nextSprite].anims.play("thrust");
                        wake.nextSprite = (wake.nextSprite + 1) & 7;
                    }
                }
            };

            wake.sprites.forEach(s => {
                s.setDepth(this.gameConfig.ship.effectsDepth).setScale(1).setTint(color).setAlpha(0.7);
            });


            const fireBolt = {
                sprite: this.scene.physics.add.sprite(this.sprite.x, this.sprite.y, 'fire-effect')
                    .setBounce(0, 0)
                    .setScale(0.3)
                    .setCollideWorldBounds(true)
                    .setDepth(this.gameConfig.ship.depth - 1)
                    .setAlpha(1),

                generate: () => {
                    fireBolt.follow();
                    if (fireBolt.sprite && fireBolt.sprite.anims) {
                        fireBolt.sprite.anims.play('fire-bolt');
                    }
                },

                follow: () => {
                    fireBolt.sprite.x = this.sprite.x;
                    fireBolt.sprite.y = this.sprite.y;
                    fireBolt.sprite.setRotation(this.sprite.rotation);
                },

                destroy: () => {
                    fireBolt.sprite.destroy();
                }
            };

            const explosion = {
                spriteBG: this.scene.physics.add.sprite(-300, -300, 'shockwave')
                    .setDepth(this.gameConfig.ship.effectsDepth)
                    .setTintFill(color)
                    .setScale(1.5)
                    .setAlpha(0.9),
                spriteFG: this.scene.physics.add.sprite(-300, -300, 'fire-effect')
                    .setBounce(0, 0)
                    .setScale(0.3)
                    .setCollideWorldBounds(true)
                    .setDepth(this.gameConfig.ship.depth - 1)
                    .setAlpha(1),

                generate: () => {
                    explosion.spriteBG.x = this.sprite.x;
                    explosion.spriteBG.y = this.sprite.y;
                    explosion.spriteBG.anims.play("explosion");

                    explosion.spriteFG.x = this.sprite.x;
                    explosion.spriteFG.y = this.sprite.y;
                    explosion.spriteFG.setRotation(this.sprite.rotation);
                    explosion.spriteFG.anims.play("fire-bolt");
                    sounds.get('explosion1').play3d(this.sprite.x, this.sprite.y);
                },

                destroy: () => {
                    explosion.spriteBG.destroy();
                    explosion.spriteFG.destroy();
                },
            };

            this.fireBolt = fireBolt;
            this.wake = wake;
            this.explosion = explosion;
            this.color = color;

            return this;
        }

        setCameraToFollow() {
            this.followed = true;
            this.scene.cameras.main.startFollow(
                this.sprite, true);
            this.sprite.setDepth(this.gameConfig.ship.depth + 1);

            let lastX = this.sprite.x;
            let lastY = this.sprite.y;
            setTimeout(() => {
                if (lastX !== this.sprite.x || lastY !== this.sprite.y) {
                    lastX = this.sprite.x;
                    lastY = this.sprite.y;
                    Object.keys(OUT_OF_VIEW_SHIPS).forEach((key) => {
                        const s = OUT_OF_VIEW_SHIPS[key];
                        if (!s.viewable && s.inView()) {
                            s.setViewable(true);
                        }
                    });
                }
            }, 500);

        }

        destroyed(isControlledPlayer) {
            this.explosion.generate();
            this.sprite
                .setActive(false)
                .setVisible(false);
            this.alive = false;
            if (isControlledPlayer) {
                if (!this.deathSound) {
                    this.deathSound = this.scene.sound.add('death1');
                }
                this.deathSound.play({'volume': 2});
            }
        }

        spawned(x, y, angle, thrustAngle) {
            this.sprite
                .setActive(true)
                .setVisible(true);
            this.alive = true;
            this.moveTo(x, y, angle, thrustAngle)
        }

        inView() {
            return this.transform.inView(this.sprite.x, this.sprite.y, this.scene.cameras.main.worldView);
        }

        setViewable(v) {
            const viewableChanged = this.viewable !== v;
            this.viewable = v;

            if (viewableChanged) {
                this.sprite
                    .setActive(this.viewable)
                    .setVisible(this.viewable);
                if (this.viewable) {
                    delete OUT_OF_VIEW_SHIPS[this.id];
                } else if (!this.followed) {
                    OUT_OF_VIEW_SHIPS[this.id] = this;
                }
            }
        }

        moveTo(x, y, angle, thrustAngle) {
            if (!this.alive) {
                return;
            }

            this.setViewable(this.followed || this.transform.inView(x, y, this.scene.cameras.main.worldView));

            if (this.viewable) {
                this.wake.generate();

                const diff = calculateAngleDistance(angle, thrustAngle);
                if (Math.abs(diff) <= Math.PI / 4) {
                    setShipThrustAnim(this.sprite, 'ship-fw');
                } else if (Math.abs(diff - Math.PI / 2) <= Math.PI / 4) {
                    setShipThrustAnim(this.sprite, 'ship-left');
                } else if (Math.abs(diff - Math.PI) <= Math.PI / 4) {
                    setShipThrustAnim(this.sprite, 'ship-back');
                } else if (Math.abs(diff + Math.PI / 2) <= Math.PI / 4) {
                    setShipThrustAnim(this.sprite, 'ship-right');
                }
            }

            this.sprite.x = x;
            this.sprite.y = y;
            this.sprite.setRotation(angle);
            if (this.viewable) {
                this.fireBolt.follow();
            }
        }


        get x() {
            return this.sprite.x;
        }


        get y() {
            return this.sprite.y;
        }


        get angle() {
            return this.sprite.angle;
        }

        destroy() {
            this.sprite.destroy();
            this.fireBolt.destroy();
            this.wake.destroy();
            this.explosion.destroy();
        }
    }

    class PlayerClass {
        constructor(scene, gameConfig, hud, transform, colors) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.hud = hud;
            this.transform = transform;
            this.moveQueue = null;
            this.colors = colors;

            this.playerId = null;
            this.name = null;
            this.displayName = null;
            this.ship = null;
            this.server = null;
        }

        create(p, sounds) {
            this.playerId = p.playerId.guid;
            this.name = p.name;
            this.displayName = p.name;
            while (this.displayName.length < this.gameConfig.hud.nameLength) {
                this.displayName += ' ';
            }

            this.ship = new Ship(this.scene, this.gameConfig, this.transform, this.colors).create(p, sounds);

            return this;
        }

        setServer(server) {
            this.server = server;
            return this;
        }

        setMoveQueue(moveQueue) {
            this.moveQueue = moveQueue;
            return this;
        }

        showExpunged() {
            this.hud.centerMessage.show('You have been booted for inactivity.\nRefresh to rejoin', this.ship.color);
        }

        showStart() {
            this.hud.centerMessage.show('Press SPACEBAR to START', this.ship.color, () => !this.ship.sprite.active);
        }

        hideStart() {
            this.hud.centerMessage.hide();
        }

        updateScore(resp) {
            this.hud.playerScore.update(resp, this.displayName, this.ship.color);
        }

        move(pointer, moveCartesian, moveRadial, forwardDir) {
            if (!this.moveQueue) {
                return;
            }
            const command = {};

            const moveX = pointer.movementX;
            const moveY = pointer.movementY;

            const x = this.ship.x + moveX;
            const y = this.ship.y + moveY;

            if (moveCartesian || moveCartesian === moveRadial) {
                command.move = transform.model(pointer.movementX, pointer.movementY);
            }
            if (moveRadial || moveCartesian === moveRadial) {
                if (Math.abs(this.ship.x - x) > 2 || Math.abs(this.ship.y - y) > 2) {
                    command.angle = {
                        "value": Phaser.Math.Angle.Between(this.ship.x, this.ship.y, x, y) + forwardDir
                    };
                }
            }
            this.moveQueue.produce(command);
        }

        spawn() {
            if (!this.ship.sprite.active) {
                this.server.sendMessage({
                    "spawn": EMPTY_OBJ
                })
            }
        }

        destroy() {
            this.ship.destroy();
        }
    }

    class PlayersClass {
        constructor(scene, sounds, gameConfig, hud, transform, playerInputs, getServer, colors) {
            this.scene = scene;
            this.sounds = sounds;
            this.gameConfig = gameConfig;
            this.hud = hud;
            this.transform = transform;
            this.playerInputs = playerInputs;
            this.getServer = getServer;
            this.colors = colors;

            this.players = {};
            this.controllableUsers = {};
            this.playerToFollow = null;
        }

        appendAction(action) {
            this.hud.latestActions.append(action);
        }

        create(p) {
            if (!this.players[p.playerId.guid]) {
                this.players[p.playerId.guid] = new PlayerClass(
                    this.scene,
                    this.gameConfig,
                    this.hud,
                    this.transform,
                    this.colors
                ).create(p, this.sounds);
            }

            return this.players[p.playerId.guid];
        }

        addControllableUser(userId) {
            this.controllableUsers[userId] = userId;
            return this;
        }

        getControllablePlayer(userId) {
            if (this.controllableUsers[userId]) {
                return Optional.of(this.players[userId])
            }
            return Optional.empty();
        }

        isControllablePlayer(userId) {
            return !!this.controllableUsers[userId];
        }

        follow(userId) {
            this.playerToFollow = userId;
            this._setCameraToFollow();
            return this;
        }

        _setCameraToFollow() {
            this.getFollowedPlayer()
                .ifPresent(p =>
                    p.ship.setCameraToFollow()
                );
        }

        getFollowedPlayer() {
            if (this.playerToFollow) {
                return Optional.of(this.players[this.playerToFollow]);
            }
            return Optional.empty();
        }

        createAnims() {
            this.scene.anims.create({
                key: "ship-fw",
                frames: this.scene.anims.generateFrameNumbers('ship', {start: 0, end: 1}),
                frameRate: 5,
                repeat: 1
            });

            this.scene.anims.create({
                key: "ship-right",
                frames: this.scene.anims.generateFrameNumbers('ship', {start: 2, end: 3}),
                frameRate: 5,
                repeat: 1
            });

            this.scene.anims.create({
                key: "ship-back",
                frames: this.scene.anims.generateFrameNumbers('ship', {start: 4, end: 5}),
                frameRate: 5,
                repeat: 1
            });

            this.scene.anims.create({
                key: "ship-left",
                frames: this.scene.anims.generateFrameNumbers('ship', {start: 6, end: 7}),
                frameRate: 5,
                repeat: 1
            });

            this.scene.anims.create({
                key: "ship-rest",
                frames: this.scene.anims.generateFrameNumbers('ship', {start: 8, end: 8}),
                frameRate: 5,
                repeat: 0
            });

            this.scene.anims.create({
                key: "thrust",
                frames: this.scene.anims.generateFrameNumbers('thrust', {start: 0, end: 8}),
                frameRate: 30,
                repeat: 0
            });

            this.scene.anims.create({
                key: "explosion",
                frames: this.scene.anims.generateFrameNumbers('shockwave', {start: 0, end: 8}),
                frameRate: 20,
                repeat: 0
            });

            this.scene.anims.create({
                key: "fire-bolt",
                frames: this.scene.anims.generateFrameNumbers('fire-effect', {start: 0, end: 3}),
                frameRate: 15,
                repeat: 0
            });

            return this;
        }

        setupQueues(queues) {

            queues.command
                .add('playersList', resp => {
                    resp.players.forEach(r => this.create(r));
                })
                .add('playerScoreUpdate', r => this.getFollowedPlayer().ifPresent(p => p.updateScore(r)));

            queues.event
                .add('playerJoined', resp => {
                    this.create(resp);

                    this.getControllablePlayer(resp.playerId.guid)
                        .ifPresent(p => {
                            p.setServer(this.getServer(p.playerId));

                            this.playerInputs.onMove = (pointer, MOVE_CARTESIAN, MOVE_RADIAL, FORWARD_DIR) =>
                                p.move(pointer, MOVE_CARTESIAN, MOVE_RADIAL, FORWARD_DIR);

                            this.playerInputs.onSpawn = () => p.spawn();

                            this.playerInputs.start();
                            if (queues.move) {
                                p.setMoveQueue(queues.move);
                            }
                            p.hideStart();
                            p.spawn();
                        });

                    if (this.playerToFollow === resp.playerId.guid) {
                        this._setCameraToFollow();
                    }
                })
                .add('playerMoved', resp => {
                    if (this.get(resp.playerId.guid)) {
                        const move = this.transform.view(resp.x, resp.y);

                        this.get(resp.playerId.guid).ship.moveTo(move.x, move.y, resp.angle, resp.thrustAngle);
                    }
                })
                .add('playerSpawned', resp => {
                    resp = resp.location;
                    if (this.get(resp.playerId.guid)) {
                        const move = this.transform.view(resp.x, resp.y);

                        this.get(resp.playerId.guid).ship.spawned(move.x, move.y, resp.angle, resp.thrustAngle);
                        this.getControllablePlayer(resp.playerId.guid)
                            .ifPresent(p => p.hideStart());
                    }
                })
                .add('playerDestroyed', resp => {
                    if (this.get(resp.playerId.guid)) {
                        const ctrlPlayer = this.getControllablePlayer(resp.playerId.guid);
                        this.get(resp.playerId.guid).ship.destroyed(ctrlPlayer.map(p => true).orElse(false));
                        ctrlPlayer.ifPresent(p => p.showStart());
                        this.appendAction({
                            "playerDestroyed": resp
                        });
                    }
                })
                .add('playerLeft', resp => {
                    this.destroyById(resp.playerId.guid);
                })
                .add('DISCONNECTED', resp => {
                    Object.keys(this.players).forEach((playerId) => {
                        const p = this.get(playerId);
                        if (p && !this.isControllablePlayer(playerId)) {
                            this.destroyById(playerId);
                        }
                    });
                });

            return this;
        }

        setupHud() {
            this.hud.setGetPlayer((id) => this.get(id));
            return this;
        }

        get(id) {
            return this.players[id];
        }

        destroyById(id) {
            const p = this.get(id);
            if (p) {
                p.destroy(id);
                if (this.isControllablePlayer(id)) {
                    p.showExpunged();
                }
            }
            delete this.players[id];
        }

    }

    let instance;

    return {
        'get': (scene, sounds, gameConfig, hud, transform, queues, playerInputs, getServer, colors) => {
            if (!instance) {
                instance = new PlayersClass(scene, sounds, gameConfig, hud, transform, playerInputs, getServer, colors)
                    .createAnims()
                    .setupQueues(queues)
                    .setupHud();
            }
            return instance;
        }
    };
})();


try {
    module.exports = Players;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}