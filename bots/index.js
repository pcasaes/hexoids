global.WebSocket = require('ws');
global.Optional = require('../src/main/resources/META-INF/resources/js/optional');
const crypto = require('crypto');
const Server = require('../src/main/resources/META-INF/resources/js/server');
const QueueConsumer = require('../src/main/resources/META-INF/resources/js/event-queue-consumer');
const GameConfig = require('../src/main/resources/META-INF/resources/js/game-config');
const Players = require('../src/main/resources/META-INF/resources/js/player');
const Transform = require('../src/main/resources/META-INF/resources/js/transform');
const AiBot = require('../src/main/resources/META-INF/resources/js/ai');
const settings = require('./settings');


const QUEUES = {
    'event': new QueueConsumer('event'),
    'command': new QueueConsumer('command')
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
                    'destroy': () => {
                    },
                    'setRotation': (r) => {
                        sprite.rotation = r;
                        return sprite;
                    },
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

const transform = Transform.get(GameConfig.get());

function getPlayers() {
    return Players.get(SCENE_MOCK, GameConfig.get(), transform, QUEUES);
}

function getServer(uuid) {
    return Server.get(uuid, QUEUES, settings.host);
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