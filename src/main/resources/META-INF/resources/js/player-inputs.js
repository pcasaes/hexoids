const PlayerInputs = (function () {

    class PlayerInputsClass {
        constructor(game) {
            this.game = game;
            this.onMove = null;
            this.onSpawn = null;
            this.onFire = null;
        }

        start() {
            // Pointer lock will only work after an 'engagement gesture', e.g. mousedown, keypress, etc.
            this.game.input.on('pointerdown', () => {

                this.game.input.mouse.requestPointerLock();

            }, this.game);


            // When locked, you will have to use the movementX and movementY properties of the pointer
            // (since a locked cursor's xy position does not update)
            this.game.input.on('pointermove', (pointer) => {

                if (this.game.input.mouse.locked && this.onMove) {
                    this.onMove(pointer, MOVE_CARTESIAN, MOVE_RADIAL, FORWARD_DIR);
                }
            }, this.game);

            // Exit pointer lock when Q is pressed. Browsers will also exit pointer lock when escape is
            // pressed.
            this.game.input.keyboard.on('keydown_Q', () => {
                if (this.game.input.mouse.locked) {
                    this.game.input.mouse.releasePointerLock();
                }
            }, this.game);

            this.game.input.keyboard.on('keydown_SPACE', () => {
                if (this.onSpawn) {
                    this.onSpawn();
                }
            }, this.game);

            let MOVE_CARTESIAN = false;
            let MOVE_RADIAL = false;
            let FORWARD_DIR = 0;

            this.game.input.keyboard.on('keydown_W', () => {
                FORWARD_DIR = 0;
            }, this.game);
            this.game.input.keyboard.on('keydown_D', () => {
                FORWARD_DIR = Math.PI / 2;
            }, this.game);
            this.game.input.keyboard.on('keydown_S', () => {
                FORWARD_DIR = Math.PI;
            }, this.game);
            this.game.input.keyboard.on('keydown_A', () => {
                FORWARD_DIR = Math.PI / -2;
            }, this.game);

            this.game.input.keyboard.on('keydown_Z', () => {
                MOVE_CARTESIAN = true;
            }, this.game);
            this.game.input.keyboard.on('keyup_Z', () => {
                MOVE_CARTESIAN = false;
            }, this.game);

            this.game.input.keyboard.on('keydown_X', () => {
                MOVE_RADIAL = true;
            }, this.game);
            this.game.input.keyboard.on('keyup_X', () => {
                MOVE_RADIAL = false;
            }, this.game);

            this.game.input.keyboard.on('keydown_C', () => {
                if (this.onFire) {
                    this.onFire();
                }
            }, this.game);
        }
    }

    let instance;

    return {
        'get': (game) => {
            if (!instance) {
                instance = new PlayerInputsClass(game);
            }
            return instance;
        }
    }
})();