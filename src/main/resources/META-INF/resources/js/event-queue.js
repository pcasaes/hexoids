class QueueConsumer {
    constructor(typeProperty) {
        this.typeProperty = typeProperty;
        this.events = {};
    }

    add(type, consumer) {
        this.events[type] = consumer;
        return this;
    }

    consume(resp) {
        const event = this.events[resp[this.typeProperty]];
        if (event) {
            event(resp);
        }
    }
}


class QueueProducer {
    constructor(sendMessage) {
        this.queue = [];
        this.sendMessage = sendMessage;
        this.intervalId = null;
    }

    produce(message) {
        this.queue.push(message);
    }

    start() {
        this.intervalId  = setInterval(() => {
            const m = {
                "command": "MOVE_PLAYER",
                "moveX": 0,
                "moveY": 0,
            };

            let hasAngle = false;
            for (let evt = this.queue.shift(); !!evt; evt = this.queue.shift()) {
                if (evt.move) {
                    m.moveX += evt.move.x;
                    m.moveY += evt.move.y;
                }
                if (evt.angle) {
                    m.angle = evt.angle.value;
                    hasAngle = true;
                }
                m.thrustAngle = evt.thrustAngle;
            }
            if (hasAngle || m.moveX !== 0 || m.moveY !== 0) {
                this.sendMessage(m);
            }
        }, 50);

        return this;
    }
}