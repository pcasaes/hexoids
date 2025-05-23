const Bolts = (function () {

    function deriveName(guid, nameLength) {
        let id = "";
        for (let i = 0; i < guid.length && id.length < nameLength; i++) {
            id += guid[i];
        }
        return id.substr(0, nameLength)
    }

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

            this.startX = null;
            this.startY = null;
            this.startTimestamp = null;
            this.endTimestamp = null;
            this.angle = null;
            this.speed = null;
            this.velX = null;
            this.velY = null;
        }

        create(b) {
            this.startX = b.x;
            this.startY = b.y;
            this.velX = Math.cos(b.angle);
            this.velY = Math.sin(b.angle);
            this.startTimestamp = b.startTimestamp;
            this.endTimestamp = b.startTimestamp + b.ttl;
            this.angle = b.angle;
            this.speed = b.speed / 1000;

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

        move(x, y) {
            const move = this.data.transform.view(x, y);

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
        constructor(server, scene, players, gameConfig, transform, sounds, clock) {
            this.data = {
                'server': server,
                'scene': scene,
                'players': players,
                'gameConfig': gameConfig,
                'transform': transform,
                'sounds': sounds,
                'clock': clock,
            };

            this.bolts = {};
            this.lastFire = 0;
        }


        fire() {
            const now = this.data.clock.clientTime();
            if (now - this.lastFire > this.data.gameConfig.bolt.debounce) {
                this.lastFire = now;
                this.data.server.sendMessage({
                    "fire": EMPTY_OBJ
                });
            }
        }

        fired(b) {
            if (!this.bolts[b.boltId.guid]) {
                const bolt = POOL.pop();

                this.bolts[b.boltId.guid] = (!bolt ? new Bolt(this.data) : bolt).create(b).fired();
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

        update() {
            const now = this.data.clock.gameTime();
            Object.keys(this.bolts).forEach((boltId) => {
                const bolt = this.bolts[boltId];
                if (bolt.endTimestamp < this.data.clock.gameTime()) {
                    this.destroyById(boltId);
                } else {
                    const velocityDelta = bolt.speed * (now - bolt.startTimestamp);

                    const newX = bolt.startX + velocityDelta * bolt.velX;
                    const newY = bolt.startY + velocityDelta * bolt.velY;

                    bolt.move(newX, newY);
                }
            });
        }

        setupQueues(queues) {
            queues.command
                .add('liveBoltsList', resp => {
                    resp.bolts.forEach(r => this.fired(r));
                });

            queues.event
                .add('boltFired', resp => {
                    this.fired(resp)
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
        'get': (server, scene, players, gameConfig, transform, sounds, clock, queues, playerInputs) => {
            if (!instance) {
                instance = new BoltsClass(server, scene, players, gameConfig, transform, sounds, clock)
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