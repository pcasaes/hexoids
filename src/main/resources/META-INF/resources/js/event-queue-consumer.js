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

try {
    module.exports = QueueConsumer;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}