const GameConfig = (function () {

    const DEADZONE_SIZE = 100;

    const HUD_ALPHA = 0.6;

    const DEPTH_BG_EFFECTS = -2;
    const DEPTH_BG = -1;
    const DEPTH_HUD = 200;

    const CALC_MEMO = {};

    const GAME_CONFIG = {
        'ship': {
            'depth': 100,
            'effectsDepth': DEPTH_BG_EFFECTS,
            'sound': {
                'max': 3,
                'distanceThreshold': 0.5,
                'debounce': 100,
            }
        },
        'bolt': {
            'debounce': 100,
            'sound': {
                'max': 3,
                'distanceThreshold': 0.5,
                'debounce': 100,
            }
        },
        'background': {
            'depth': DEPTH_BG,
            'effectsDepth': DEPTH_BG_EFFECTS,
        },
        'hud': {
            'nameLength': 8,
            'font': {
                'size': {
                    'center':
                        28,
                    'periphery':
                        20,
                },
                'scale': {
                    'width': 1.8
                },
                'offset': {
                    'x': -2,
                    'y': -4
                }
            },
            'alpha': HUD_ALPHA,
            'depth': DEPTH_HUD,
        },
        'world': {
            'renderOffset': 128,
            'min': {
                'x': 0,
                'y': 0,
            },
            'max': {
                'x': 10000,
                'y': 10000,
            }
        },
        'camera': {
            'deadZone': DEADZONE_SIZE,
        },

        'add': (field, val) => {
            if (!CALC_MEMO[field]) {
                CALC_MEMO[field] = {};
            }
            if (!CALC_MEMO[field][val]) {
                let p = GAME_CONFIG;
                field.split('.').forEach(v => p = p[v]);
                CALC_MEMO[field][val] = p + val;
            }
            return CALC_MEMO[field][val];
        }
    };

    GAME_CONFIG.world['width'] = GAME_CONFIG.world.max.x - GAME_CONFIG.world.min.x;
    GAME_CONFIG.world['height'] = GAME_CONFIG.world.max.y - GAME_CONFIG.world.min.y;

    GAME_CONFIG.world['width_float'] = 1.0 * GAME_CONFIG.world.width;
    GAME_CONFIG.world['height_float'] = 1.0 * GAME_CONFIG.world.height;

    return {
        'get': () => GAME_CONFIG
    };
})();

try {
    module.exports = GameConfig;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}
