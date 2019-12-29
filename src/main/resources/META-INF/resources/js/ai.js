const AI = (function () {

    const ai = {intervals: []};

    const DIRS = [
        0,
        Math.PI / 2,
        Math.PI,
        Math.PI / -2
    ];

    function getDirection(dim) {
        const sprite = players[USER_ID].sprite;

        if (sprite[dim] < 20) {
            return Math.random() * 5;
        } else if (sprite[dim] > BOUNDS.max[dim] - 20) {
            return Math.random() * -5;
        } else {

            const d = (BOUNDS.max[dim] / 2 - sprite[dim]) / BOUNDS.max[dim];

            return ((Math.random() * (1 - d) + d) - 0.5) * 10;
        }
    }

    function start() {
        ai.intervals.push(
            setInterval(() => {
                if (Math.random() <= 0.2) {
                    ai.x = getDirection('x');
                    ai.y = getDirection('y');

                    ai.forwardDir = DIRS[Math.floor(Math.random() * 4)];
                }

                if (Math.random() <= 0.15) {
                    sendMessage({
                        "command": "FIRE_BOLT"
                    })
                }

            }, 100)
        );

        ai.intervals.push(
            setInterval(() => {
                const command = {};
                const sprite = players[USER_ID].sprite;

                const moveX = ai.x;
                const moveY = ai.y;

                const x = sprite.x + moveX;
                const y = sprite.y + moveY;

                command.move = transform.model(moveX, moveY);
                if (Math.abs(sprite.x - x) > 2 || Math.abs(sprite.y - y) > 2) {
                    command.angle = {
                        "value": Phaser.Math.Angle.Between(sprite.x, sprite.y, x, y) + ai.forwardDir
                    };
                    command.thrustAngle = ai.forwardDir;
                }

                moveQueue.push(command);
            }, 20)
        );
    }

    function stop() {
        ai.intervals.forEach(v => clearTimeout(v));
        ai.intervals = [];
    }

    return {
        'start': start,
        'stop': stop
    }
})();