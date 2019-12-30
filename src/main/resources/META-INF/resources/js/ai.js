class AiBot {
    constructor(userId) {
        this.userId = userId;
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
            const sprite = PLAYERS[this.userId].sprite;

            if (sprite[dim] < 20) {
                return Math.random() * 5;
            } else if (sprite[dim] > BOUNDS.max[dim] - 20) {
                return Math.random() * -5;
            } else {

                const d = (BOUNDS.max[dim] / 2 - sprite[dim]) / BOUNDS.max[dim];

                return ((Math.random() * (1 - d) + d) - 0.5) * 10;
            }
        };

        this.intervals.push(
            setInterval(() => {
                if (!PLAYERS[this.userId]) {
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
                if (!PLAYERS[this.userId]) {
                    return;
                }

                const command = {};
                const sprite = PLAYERS[this.userId].sprite;

                const moveX = this.x;
                const moveY = this.y;

                const x = sprite.x + moveX;
                const y = sprite.y + moveY;

                command.move = transform.model(moveX, moveY);
                if (Math.abs(sprite.x - x) > 2 || Math.abs(sprite.y - y) > 2) {
                    command.angle = {
                        "value": Phaser.Math.Angle.Between(sprite.x, sprite.y, x, y) + this.forwardDir
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
