const Server = (function () {

    let lastMessageReceived;

    let backOff = 50;

    const synDto = {
        "dto": "event",
        "event": null
    };


    class ServerClass {
        constructor(userId, queues, host, proto) {
            this.userId = userId;
            this.queues = queues;
            this.socket = null;
            this.host = host;
            this.booted = false;
            this.proto = proto;
        }

        createSocket() {
            const endpoint = "ws://" + this.host + "/game/" + this.userId;
            console.log(endpoint);
            this.socket = new WebSocket(endpoint);
            this.socket.binaryType = "arraybuffer";
            this.socket.onopen = () => {
                console.log("Connected to the web socket");
            };
            this.socket.onclose = () => {
                console.log("web socket closed");
                this.queues.event.consume({
                    "event": {
                        "type": "DISCONNECTED"
                    }
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
                lastMessageReceived = this.proto.readDto(m.data);
                if (lastMessageReceived.dto === 'events') {
                    lastMessageReceived
                        .events
                        .events
                        .forEach(ev => {
                            synDto.event = ev;
                            this.queues.event.consume(synDto);
                        });
                } else {
                    this.queues.event.consume(lastMessageReceived);
                    this.queues.command.consume(lastMessageReceived);
                }
            };
        }

        setup() {
            this.createSocket();

            this.queues.event.add('playerLeft', resp => {
                if (resp.playerId.guid === this.userId) {
                    console.log('Player left or booted. Disconnecting');
                    this.booted = true;
                    this.socket.close();
                }
            });

            return this;
        }

        sendMessage(value) {
            if (this.socket.readyState === 1) {
                const b = this.proto.writeRequestCommand(value);
                this.socket.send(b);
            }
        }
    }

    const instances = {};

    return {
        'get': (userId, queues, host, proto) => {
            if (!instances[userId]) {
                instances[userId] = new ServerClass(userId, queues, host, proto).setup();
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