class EventQueue {
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