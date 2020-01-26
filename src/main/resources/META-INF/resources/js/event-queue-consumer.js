class QueueConsumer {
    constructor(dtoTypeProperty, typeProperty) {
        this.dtoTypeProperty = dtoTypeProperty;
        this.typeProperty = typeProperty;
        this.events = {};
    }

    add(type, consumer) {
        if (!this.events[type]) {
            this.events[type] = [];
        }
        this.events[type].push(consumer);
        return this;
    }

    consume(resp) {
        if (resp.dto === this.dtoTypeProperty) {
            const event = this.events[resp[this.dtoTypeProperty][this.typeProperty]];
            if (event) {
                event.forEach(ev => ev(resp[this.dtoTypeProperty][resp[this.dtoTypeProperty][this.typeProperty]]));
            }
        }
    }
}

try {
    module.exports = QueueConsumer;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}