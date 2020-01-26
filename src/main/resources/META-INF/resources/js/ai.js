const AiBot = (function() {

    const EMPTY_OBJ = {};

    class AiBotClass {
        constructor(userId, server, players, transform, gameConfig) {
            this.userId = userId;
            this.server = server;
            this.players = players;
            this.transform = transform;
            this.gameConfig = gameConfig;
            this.intervals = [];
            this.x = 0;
            this.y = 0;
            this.forwardDir = 0;
            this.waitingToSpawn = true;
        }

        start() {
            const DIRS = [
                0,
                Math.PI / 2,
                Math.PI,
                Math.PI / -2
            ];

            const getDirection = (dim) => {
                return this.players.getControllablePlayer(this.userId)
                    .ifPresent(p => {
                            const ship = p.ship;

                            if (ship[dim] < 20) {
                                return Math.random() * 8;
                            } else if (ship[dim] > this.gameConfig.world.max[dim] - 20) {
                                return Math.random() * -8;
                            } else {

                                const d = (this.gameConfig.world.max[dim] / 2 - ship[dim]) / this.gameConfig.world.max[dim];

                                return ((Math.random() * (1 - d) + d) - 0.5) * 16;
                            }
                        },
                        () => this[dim]);

            };

            this.intervals.push(
                setInterval(() => {
                    this.players.getControllablePlayer(this.userId)
                        .ifPresent(p => {
                            if (Math.random() <= 0.2) {
                                this.x = getDirection('x');
                                this.y = getDirection('y');

                                this.forwardDir = DIRS[Math.floor(Math.random() * 4)];
                            }

                            if (Math.random() <= 0.15) {
                                this.server.sendMessage({
                                    "fire": EMPTY_OBJ
                                })
                            }
                        });

                }, 100)
            );

            this.intervals.push(
                setInterval(() => {
                    this.players.getControllablePlayer(this.userId)
                        .ifPresent(p => {
                            if (p.ship.sprite.active) {
                                const m = {
                                    "moveX": 0,
                                    "moveY": 0,
                                };

                                const ship = p.ship;

                                const moveX = this.x;
                                const moveY = this.y;

                                const x = ship.x + moveX;
                                const y = ship.y + moveY;

                                const move = this.transform.model(moveX, moveY);
                                m.moveX = move.x;
                                m.moveY = move.y;
                                let hasAngle = false;
                                if (Math.abs(ship.x - x) > 2 || Math.abs(ship.y - y) > 2) {
                                    m.angle = {
                                        "value": Math.atan2(y - ship.y, x - ship.x) + this.forwardDir
                                    };
                                    m.thrustAngle = {
                                        "value": this.forwardDir
                                    };
                                    hasAngle = true;
                                }

                                if (hasAngle || m.moveX !== 0 || m.moveY !== 0) {
                                    this.server.sendMessage({
                                        "move": m
                                    });
                                }
                            } else if (this.waitingToSpawn) {
                                this.waitingToSpawn = false;
                                setTimeout(() => {
                                    this.waitingToSpawn = true;
                                    this.server.sendMessage({
                                        "spawn": EMPTY_OBJ
                                    })
                                }, 1000);
                            }
                        });


                }, 50)
            );
        }

        stop() {
            this.intervals.forEach(v => clearTimeout(v));
            this.intervals = [];
        }
    }

    return AiBotClass;
})();

try {
    module.exports = AiBot;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}