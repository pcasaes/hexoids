const Transform = (function () {

    class TransformClass {
        constructor(gameConfig) {
            this.gameConfig = gameConfig;
        }

        model(x, y) {
            return {
                "x": x / this.gameConfig.world.width_float,
                "y": y / this.gameConfig.world.height_float,
            }
        }

        view(x, y) {
            return {
                "x": x * this.gameConfig.world.width_float,
                "y": y * this.gameConfig.world.height_float
            }
        }

        inView(x, y, worldView) {
            return worldView.x - this.gameConfig.world.renderOffset < x &&
                worldView.y - this.gameConfig.world.renderOffset < y &&
                worldView.x + worldView.width + this.gameConfig.world.renderOffset > x &&
                worldView.y + worldView.height + this.gameConfig.world.renderOffset > y;
        }
    }

    let instance;

    return {
        'get': (gameConfig) => {
            if (!instance) {
                instance = new TransformClass(gameConfig);
            }
            return instance;
        }
    }
})();

try {
    module.exports = Transform;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}