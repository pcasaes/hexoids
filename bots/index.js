const settings = require('./settings');
const {Worker, isMainThread} = require('worker_threads');

if (isMainThread) {
    const threads = [];
    for (let i = 0; i < settings.workers; i++) {
        threads.push(new Worker(__filename));

    }
} else {
    global.WebSocket = require('ws');
    global.Optional = require('../src/main/resources/META-INF/resources/js/optional');
    const crypto = require('crypto');
    const Server = require('../src/main/resources/META-INF/resources/js/server');
    const QueueConsumer = require('../src/main/resources/META-INF/resources/js/event-queue-consumer');
    const GameConfig = require('../src/main/resources/META-INF/resources/js/game-config');
    const Players = require('../src/main/resources/META-INF/resources/js/player');
    const Colors = require('../src/main/resources/META-INF/resources/js/color');
    const Users = require('../src/main/resources/META-INF/resources/js/user');
    const Transform = require('../src/main/resources/META-INF/resources/js/transform');
    const AiBot = require('../src/main/resources/META-INF/resources/js/ai');
    const Clock = require('../src/main/resources/META-INF/resources/js/clock');
    const ProtoProcessor = require('../src/main/js-proto/main');


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

    function getUsers() {
        return Users.get(GameConfig.get(), null, genUuid);
    }

    function getClock() {
        return Clock;
    }

    function getColors() {
        return Colors;
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
            'centerMessage': {
                'show': () => {
                },
                'hide': () => {
                },
            },
            'latestActions': {
                'append': () => {
                },
            }
        };
    }

    function getPlayers() {
        return Players.get(
            SCENE_MOCK,
            getSounds(),
            GameConfig.get(),
            getHud(),
            transform,
            QUEUES,
            getPlayerInputs(),
            getServer,
            getColors());
    }

    function getServer(userId) {
        return Server.get(getUsers().get(userId), QUEUES, settings.host, ProtoProcessor, getClock());
    }

    const BOTS = [];

    /**
     *
     * @returns {number} 0 - do nothing, 1 - add bot, -1 - remove bot
     */
    function takeAction() {
        const shouldBe = settings.botsPerWorker * settings.workers;
        const current = getPlayers().count();
        if (current < shouldBe && BOTS.length < settings.botsPerWorker) {
            return 1;
        } else if (current > shouldBe && BOTS.length > 0) {
            return -1;
        }
        return 0;
    }

    function startUpOne() {
        const USER = getUsers().get(genUuid());
        console.log("user id " + USER.get());


        getPlayers()
            .addControllableUser(USER.get());

        const bot = new AiBot(USER,
            getServer(USER.get()),
            getPlayers()
                .addControllableUser(USER.get()),
            transform,
            GameConfig.get());

        bot.start();
        BOTS.push(bot);
    }

    function startUpAll() {
        for (let i = 0; i < settings.botsPerWorker; i++) {
            startUpOne();
        }
    }

    startUpAll();

    function startUpCountChecker() {
        setInterval(() => {
            let action = takeAction();
            if (action < 0) {
                console.log(`Reducing bots from ${BOTS.length}`);
                let bot = BOTS.pop();
                bot.stop();
            } else if (action > 0) {
                console.log(`Increasing bots from ${BOTS.length}`)
                startUpOne()
            }

            if (BOTS.length > 0 && Math.random() <= 0.05) {
                console.log(`Randomly removing one bot`);
                let bot = BOTS.pop();
                bot.stop();
            }

        }, 10000);
    }

    setTimeout(() => startUpCountChecker(), Math.random() * 10);


}