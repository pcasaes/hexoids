const Server = (function () {

    let lastMessageSent;
    let lastMessageReceived;

    let backOff = 50;

    class ServerClass {
        constructor(userId, queues, host) {
            this.userId = userId;
            this.queues = queues;
            this.socket = null;
            this.host = host;
            this.booted = false;
        }

        createSocket() {
            this.socket = new WebSocket("ws://" + this.host + "/game/" + this.userId);
            this.socket.onopen = () => {
                console.log("Connected to the web socket");
            };
            this.socket.onclose = () => {
                console.log("web socket closed");
                this.queues.event.consume({
                    "event" : "DISCONNECTED"
                });
                setTimeout(() => {
                    if (!this.booted) {
                        backOff = Math.min(5000, backOff * 2);
                        this.createSocket();
                    }
                }, backOff);
            };

            this.socket.onerror = () => {
                this.socket.close();
            };

            this.socket.onmessage = (m) => {
                backOff = 50;
                //console.log("Got message: " + m.data);
                lastMessageReceived = JSON.parse(m.data);
                this.queues.event.consume(lastMessageReceived);
                this.queues.command.consume(lastMessageReceived);
            };
        }

        setup() {
            this.createSocket();

            this.queues.event.add('PLAYER_LEFT', resp => {
                if (resp.playerId === this.userId) {
                    console.log('Player left or booted. Disconnecting');
                    this.booted = true;
                    this.socket.close();
                }
            });

            return this;
        }

        sendMessage(value) {
            if (this.socket.readyState === 1) {
                lastMessageSent = JSON.stringify(value);
                //console.log("Sending " + lastMessageSent);
                this.socket.send(lastMessageSent);
            }
        }
    }

    const instances = {};

    return {
        'get': (userId, queues, host) => {
            if (!instances[userId]) {
                instances[userId] = new ServerClass(userId, queues, host).setup();
            }
            return instances[userId];
        }
    }

})();

try {
    module.exports = Server;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}