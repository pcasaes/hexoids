const Bolts = (function () {

    const EMPTY_OBJ = {};

    const PULSE = [0xfffff, 0x888888, 0x888888, 0x000000];

    const POOL = [];

    class Bolt {
        constructor(data) {
            this.data = data;
            this.sprite = null;
            this.bg = null;
            this.pulsePos = 0;
            this.owner = null;
            this.color = null;

            this.isNew = true;
            this.viewable = false;
        }

        create(b) {
            const move = this.data.transform.view(b.x, b.y);

            if (this.isNew) {
                this.sprite = this.data.scene.physics.add.image(move.x, move.y, 'bolt');
                this.bg = this.data.scene.physics.add.image(move.x, move.y, 'bolt');
            } else {
                this.sprite.x = move.x;
                this.sprite.y = move.y;
            }

            this.owner = this.data.players.get(b.ownerPlayerId.guid);
            if (this.owner) {
                const color = this.owner.ship.color.toRgbNumber();
                this.sprite.setTint(color, color, color, color);
                this.bg.setTint(color, color, color, color);
                this.owner.ship.fireBolt.generate();
                this.color = color;
            } else {
                this.color = 0xffffff;
            }

            if (this.isNew) {
                this.sprite.setBounce(0, 0);
                this.sprite.setScale(0.15);
                this.sprite.setDepth(this.data.gameConfig.add('ship.depth', 1));
                this.sprite.setCollideWorldBounds(true);

                this.bg.setBounce(0, 0);
                this.bg.setScale(1);
                this.bg.setCollideWorldBounds(true);
                this.bg.setDepth(this.data.gameConfig.background.effectsDepth);
                this.bg.setAlpha(0.7);
                this.isNew = false;
            } else {
                this.sprite
                    .setActive(true)
                    .setVisible(true);
                this.bg
                    .setActive(true)
                    .setVisible(true);
            }
            this.viewable = true;

            return this;
        }

        fired() {
            if (this.owner) {
                this.data.sounds.get('fire1').play3d(this.owner.ship.x, this.owner.ship.y, this.data.players.isControllablePlayer(this.owner.playerId));
            }

            return this;
        }

        move(b) {
            const move = this.data.transform.view(b.x, b.y);

            const newViewable = this.data.transform.inView(move.x, move.y, this.data.scene.cameras.main.worldView);
            const viewableChanged = this.viewable !== newViewable;
            this.viewable = newViewable;

            if (viewableChanged) {
                this.sprite
                    .setActive(this.viewable)
                    .setVisible(this.viewable);
                this.bg
                    .setActive(this.viewable)
                    .setVisible(this.viewable);
            }


            if (this.viewable) {
                this.sprite.x = move.x;
                this.sprite.y = move.y;
                this.bg.x = move.x;
                this.bg.y = move.y;
                this.sprite.setTint(
                    this.color | PULSE[this.pulsePos],
                    this.color | PULSE[(this.pulsePos + 1) & 3],
                    this.color | PULSE[(this.pulsePos + 2) & 3],
                    this.color | PULSE[(this.pulsePos + 3) & 3],
                );
                this.pulsePos++;
            }
        }

        destroy() {
            this.sprite
                .setActive(false)
                .setVisible(false);
            this.bg
                .setActive(false)
                .setVisible(false);

            POOL.push(this);
        }
    }

    class BoltsClass {
        constructor(server, scene, players, gameConfig, transform, sounds) {
            this.data = {
                'server': server,
                'scene': scene,
                'players': players,
                'gameConfig': gameConfig,
                'transform': transform,
                'sounds': sounds,
            };

            this.bolts = {};
            this.lastFire = 0;
        }


        fire() {
            if (Date.now() - this.lastFire > this.data.gameConfig.bolt.debounce) {
                this.lastFire = Date.now();
                this.data.server.sendMessage({
                    "fire": EMPTY_OBJ
                });
            }
        }

        move(b) {
            if (!this.bolts[b.boltId.guid]) {
                const bolt = POOL.pop();

                this.bolts[b.boltId.guid] = (!bolt ? new Bolt(this.data) : bolt).create(b).fired();
            } else {
                this.bolts[b.boltId.guid].move(b);
            }
        }

        setupSounds() {
            this.data.sounds.get('fire1').create(
                this.data.gameConfig.bolt.sound.max,
                this.data.gameConfig.bolt.sound.debounce,
                this.data.gameConfig.bolt.sound.distanceThreshold
            );
            return this;
        }

        setupQueues(queues) {

            queues.event
                .add('boltMoved', resp => {
                    this.move(resp)
                })
                .add('boltExhausted', resp => {
                    this.destroyById(resp.boltId.guid);
                })
                .add('DISCONNECTED', resp => {
                    Object.keys(this.bolts).forEach((boltId) => this.destroyById(boltId));
                });

            return this;
        }

        setupPlayerInputs(playerInputs) {
            playerInputs.onFire = () => this.fire();

            return this;
        }

        destroyById(boltId) {
            if (this.bolts[boltId]) {
                this.bolts[boltId].destroy();
                delete this.bolts[boltId];
            }
        }
    }

    let instance;

    return {
        'get': (server, scene, players, gameConfig, transform, sounds, queues, playerInputs) => {
            if (!instance) {
                instance = new BoltsClass(server, scene, players, gameConfig, transform, sounds)
                    .setupSounds()
                    .setupPlayerInputs(playerInputs)
                    .setupQueues(queues);
            }
            return instance;
        }
    };
})();

try {
    module.exports = Bolts;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}