global.WebSocket = require('ws');
global.Optional = require('../src/main/resources/META-INF/resources/js/optional');
const crypto = require('crypto');
const Server = require('../src/main/resources/META-INF/resources/js/server');
const QueueConsumer = require('../src/main/resources/META-INF/resources/js/event-queue-consumer');
const GameConfig = require('../src/main/resources/META-INF/resources/js/game-config');
const Players = require('../src/main/resources/META-INF/resources/js/player');
const Transform = require('../src/main/resources/META-INF/resources/js/transform');
const AiBot = require('../src/main/resources/META-INF/resources/js/ai');
const ProtoProcessor = require('../src/main/js-proto/main');
const settings = require('./settings');


const QUEUES = {
    'event': new QueueConsumer('event', 'event'),
    'command': new QueueConsumer('directedCommand', 'command')
};

function genUuid() {
    const flat = crypto.randomBytes(16).toString('hex');
    return flat.substr(0, 8) + '-' +
        flat.substr(8, 4) + '-' +
        flat.substr(12, 4) + '-' +
        flat.substr(16, 4) + '-' +
        flat.substr(20);

}


const SCENE_MOCK = {
    'anims': {
        'create': () => {
        },
        'generateFrameNumbers': () => {
        },
    },
    'cameras': {
        'main': {
            'startFollow': () => {
            },
        },
    },
    'sound': {
        'add': (s) => {
            return {
                'play': () => {
                }
            }
        }
    },
    'physics': {
        'add': {
            'sprite': (x, y) => {
                const setterMock = () => {
                    return sprite;
                };
                const sprite = {
                    'x': x,
                    'y': y,
                    'rotation': 0,
                    'active': true,
                    'destroy': () => {
                    },
                    'setRotation': (r) => {
                        sprite.rotation = r;
                        return sprite;
                    },
                    'setActive': (v) => {
                        sprite.active = v;
                        return sprite;
                    },
                    'setVisible': setterMock,
                    'setTint': setterMock,
                    'setBounce': setterMock,
                    'setScale': setterMock,
                    'setDepth': setterMock,
                    'setAlpha': setterMock,
                    'setTintFill': setterMock,
                    'setCollideWorldBounds': setterMock,
                    'anims': {
                        'play': () => {
                        },
                        'getProgress': () => {
                            return 0;
                        },
                    }
                };

                return sprite;
            },
        },
    },
    'add': {
        'bitmapText': () => {
            const setterMock = () => {
                return text;
            };
            const text = {
                'setActive': setterMock,
                'setVisible': setterMock,
                'setScrollFactor': setterMock,
                'setDepth': setterMock,
                'setAlpha': setterMock,
                'setTintFill': setterMock,
                'setText': setterMock,
            };

            return text;
        },
    },
    'game': {
        'config': {
            'with': 0,
        },
    },
};

const playerInputsMock = {
    'start': () => {
    }
};

const transform = Transform.get(GameConfig.get());

transform.inView = () => false;

function getPlayerInputs() {
    return playerInputsMock;
}

function getSounds() {
    return {
        'get': () => {
            return {
                'preload': () => {
                },
                'create': () => {
                },
                'play3d': () => {
                },
                'play': () => {
                },
            }
        }
    }
}

function getHud() {
    return {
        'showCenterMessage': () => {
        },
        'hideCenterMessage': () => {
        },
    }
}

function getPlayers() {
    return Players.get(SCENE_MOCK, getSounds(), GameConfig.get(), getHud(), transform, QUEUES, getPlayerInputs(), getServer);
}

function getServer(uuid) {
    return Server.get(uuid, QUEUES, settings.host, ProtoProcessor);
}

for (let i = 0; i < settings.bots; i++) {
    const UUID = genUuid();
    console.log("user id " + UUID);


    getPlayers()
        .addControllableUser(UUID);

    const bot = new AiBot(UUID,
        getServer(UUID),
        getPlayers()
            .addControllableUser(UUID),
        transform,
        GameConfig.get());

    bot.start();
}