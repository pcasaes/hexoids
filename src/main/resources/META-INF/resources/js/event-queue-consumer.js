class QueueConsumer {
    constructor(typeProperty) {
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
        const event = this.events[resp[this.typeProperty]];
        if (event) {
            event.forEach(ev => ev(resp));
        }
    }
}

try {
    module.exports = QueueConsumer;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}