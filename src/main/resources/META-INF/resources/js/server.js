const Server = (function () {

    let lastMessageSent;
    let lastMessageReceived;

    class ServerClass {
        constructor(userId, queues) {
            this.userId = userId;
            this.queues = queues;
            this.socket = null;
        }

        setup() {
            this.socket = new WebSocket("ws://" + location.host + "/game/" + this.userId);
            this.socket.onopen = () => {
                console.log("Connected to the web socket");
            };
            this.socket.onmessage = (m) => {
                console.log("Got message: " + m.data);
                lastMessageReceived = JSON.parse(m.data);
                this.queues.event.consume(lastMessageReceived);
                this.queues.command.consume(lastMessageReceived);
            };
            return this;
        }

        sendMessage(value) {
            lastMessageSent = JSON.stringify(value);
            console.log("Sending " + lastMessageSent);
            this.socket.send(lastMessageSent);
        };
    }

    let instance = null;

    return {
        'get': (userId, queues) => {
            if (!instance) {
                instance = new ServerClass(userId, queues).setup();
            }
            return instance;
        }
    }

})();