const Bolts = (function () {


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
        }

        create(b) {
            const move = this.data.transform.view(b.x, b.y);

            if (this.isNew) {
                this.sprite = this.data.scene.physics.add.image(move.x, move.y, 'bolt');
                this.bg = this.data.scene.physics.add.image(move.x, move.y, 'bolt');
            }

            this.owner = this.data.players.get(b.ownerPlayerId);
            if (this.owner) {
                this.sprite.setTint(this.owner.ship.color, this.owner.ship.color, this.owner.ship.color, this.owner.ship.color);
                this.bg.setTint(this.owner.ship.color, this.owner.ship.color, this.owner.ship.color, this.owner.ship.color);
                this.owner.ship.fireBolt.generate();
                this.color = this.owner.ship.color;
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

            return this;
        }
        
        move(b) {
            const move = this.data.transform.view(b.x, b.y);

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

    class Bolts {
        constructor(scene, players, gameConfig, transform) {
            this.data = {
                'scene': scene,
                'players': players,
                'gameConfig': gameConfig,
                'transform': transform,
            };

            this.bolts = {};
        }

        move(b) {
            if (!this.bolts[b.boltId]) {
                const bolt = POOL.pop();

                this.bolts[b.boltId] = (!bolt ? new Bolt(this.data): bolt).create(b);
            } else {
                this.bolts[b.boltId].move(b);
            }
        }

        setupQueues(queues) {

            queues.event
                .add('BOLT_MOVED', resp => {
                    this.move(resp)
                })
                .add('BOLT_EXHAUSTED', resp => {
                    this.destroyById(resp.boltId);
                });

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
        'get': (scene, players, gameConfig, transform, queues) => {
            if (!instance) {
                instance = new Bolts(scene, players, gameConfig, transform).setupQueues(queues);
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