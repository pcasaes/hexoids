const Clock = (function() {

    class ClockClass {
        constructor() {
            this.offset = 0;
        }

        clientTime() {
            return Date.now();
        }

        gameTime() {
            return Date.now() + this.offset;
        }

        onClockSync(requestTime, sync) {
            this.offset = sync.time - (requestTime + (this.clientTime() - requestTime) / 2);
            console.log('Clock: client to server offset = ' + this.offset)
        }
    }

    return new ClockClass();
})();


try {
    module.exports = Clock;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}