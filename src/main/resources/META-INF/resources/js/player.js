const Player = (function () {

    const CONV_RADIANS_TO_DEGREE = 180 / Math.PI;


    const SHIP_COLOR = [
        0xdd0055,
        0x00aa88,
        0x3333ff,
        0xcc9900,
        0x00a0bb,
        0xaa00ff,
    ];

    function getColorFromShip(ship) {
        if (ship < 0 || ship >= SHIP_COLOR.length) {
            return SHIP_COLOR[SHIP_COLOR.length - 1];
        }
        return SHIP_COLOR[ship];
    }

    class Ship {
        constructor(scene, gameConfig, transform) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.transform = transform;

            this.sprite = null;
            this.wake = null;
            this.fireBolt = null;
            this.explosion = null;
            this.color = null;
        }

        create(p) {
            const move = this.transform.view(p.x, p.y);
            this.sprite = this.scene.physics.add.sprite(move.x, move.y, 'ship');
            const color = getColorFromShip(p.ship);
            this.sprite.setTint(color | 0x555555, color, color | 0x555555, color);
            this.sprite.setBounce(0, 0);
            this.sprite.setScale(0.3);
            this.sprite.setDepth(this.gameConfig.ship.depth);
            this.sprite.setCollideWorldBounds(true);


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
                    fireBolt.sprite.anims.play('fire-bolt');
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

        moveTo(x, y, angle) {
            this.sprite.x = x;
            this.sprite.y = y;
            this.sprite.setRotation(angle);
            this.fireBolt.follow();
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
        constructor(scene, gameConfig, transform) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.transform = transform;
            this.moveQueue = null;

            this.playerId = null;
            this.ship = null;
            this.scoreView = null;
        }

        create(p) {
            this.playerId = p.playerId;

            this.ship = new Ship(this.scene, this.gameConfig, this.transform).create(p);

            return this;
        }

        setMoveQueue(moveQueue) {
            this.moveQueue = moveQueue;
            return this;
        }


        updateScore(resp) {
            if (!this.scoreView) {
                const text = this.scene.add.bitmapText(0, 4, 'font', '', 16);
                text.setScrollFactor(0);
                text.setAlpha(this.gameConfig.hud.alpha);
                text.setDepth(this.gameConfig.hud.depth);
                text.setTintFill(this.ship.color);

                this.scoreView = text;
            }

            this.scoreView.setText(this.playerId.substr(0, 7) + ': ' + resp.score);
            this.scoreView.x = this.scene.game.config.width - (this.scoreView.width + 5);
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
                    command.thrustAngle = forwardDir;
                }
            } else if (command.move) {
                command.thrustAngle = Phaser.Math.Angle.ShortestBetween(
                    this.ship.angle,
                    Phaser.Math.Angle.Between(this.ship.x, this.ship.y, x, y) * CONV_RADIANS_TO_DEGREE
                ) / -CONV_RADIANS_TO_DEGREE;
            }
            this.moveQueue.push(command);
        }

        destroy() {
            this.ship.destroy();
        }
    }


    return {
        'create': (scene, gameConfig, transform, playerData) => new PlayerClass(scene, gameConfig, transform).create(playerData)
    };
})();

