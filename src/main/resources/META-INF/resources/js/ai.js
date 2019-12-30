class AiBot {
    constructor(userId, players, gameConfig) {
        this.userId = userId;
        this.players = players;
        this.gameConfig = gameConfig;
        this.intervals = [];
        this.x = 0;
        this.y = 0;
        this.forwardDir = 0;
    }

    start() {
        const DIRS = [
            0,
            Math.PI / 2,
            Math.PI,
            Math.PI / -2
        ];

        const getDirection = (dim) => {
            const ship = this.players.myPlayer.ship;

            if (ship[dim] < 20) {
                return Math.random() * 5;
            } else if (ship[dim] > this.gameConfig.world.max[dim] - 20) {
                return Math.random() * -5;
            } else {

                const d = (this.gameConfig.world.max[dim] / 2 - ship[dim]) / this.gameConfig.world.max[dim];

                return ((Math.random() * (1 - d) + d) - 0.5) * 10;
            }
        };

        this.intervals.push(
            setInterval(() => {
                if (!this.players.myPlayer) {
                    return;
                }


                if (Math.random() <= 0.2) {
                    this.x = getDirection('x');
                    this.y = getDirection('y');

                    this.forwardDir = DIRS[Math.floor(Math.random() * 4)];
                }

                if (Math.random() <= 0.15) {
                    sendMessage({
                        "command": "FIRE_BOLT"
                    })
                }

            }, 100)
        );

        this.intervals.push(
            setInterval(() => {
                if (!this.players.myPlayer) {
                    return;
                }

                const command = {};
                const ship = this.players.myPlayer.ship;

                const moveX = this.x;
                const moveY = this.y;

                const x = ship.x + moveX;
                const y = ship.y + moveY;

                command.move = transform.model(moveX, moveY);
                if (Math.abs(ship.x - x) > 2 || Math.abs(ship.y - y) > 2) {
                    command.angle = {
                        "value": Phaser.Math.Angle.Between(ship.x, ship.y, x, y) + this.forwardDir
                    };
                    command.thrustAngle = this.forwardDir;
                }

                moveQueue.push(command);
            }, 20)
        );
    }

    stop() {
        this.intervals.forEach(v => clearTimeout(v));
        this.intervals = [];
    }
}
