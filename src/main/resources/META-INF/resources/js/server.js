const Server = (function () {

    let lastMessageSent;
    let lastMessageReceived;

    class ServerClass {
        constructor(userId, queues, host) {
            this.userId = userId;
            this.queues = queues;
            this.socket = null;
            this.host = host
        }

        setup() {
            this.socket = new WebSocket("ws://" + this.host + "/game/" + this.userId);
            this.socket.onopen = () => {
                console.log("Connected to the web socket");
            };
            this.socket.onmessage = (m) => {
                //console.log("Got message: " + m.data);
                lastMessageReceived = JSON.parse(m.data);
                this.queues.event.consume(lastMessageReceived);
                this.queues.command.consume(lastMessageReceived);
            };

            this.queues.event.add('PLAYER_LEFT', resp => {
                if (resp.playerId === this.userId) {
                    console.log('Player left or booted. Disconnecting');
                    this.socket.close();
                }
            });

            return this;
        }

        sendMessage(value) {
            lastMessageSent = JSON.stringify(value);
            //console.log("Sending " + lastMessageSent);
            this.socket.send(lastMessageSent);
        };
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