const Hud = (function () {
    class HudClass {
        
        constructor(scene, gameConfig) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.centerMessage = null;
        }


        showCenterMessage(message, color, showIfFunction) {
            if (!this.centerMessage) {
                this.centerMessage = [];
                this.centerMessage.push(this.scene.add.bitmapText(-100, -100, 'font', message, 16));
                this.centerMessage.push(this.scene.add.bitmapText(-100, -100, 'font', message, 16));

                this.centerMessage.forEach(text => {
                    text.setScrollFactor(0);
                });

                this.centerMessage[0]
                    .setAlpha(this.gameConfig.hud.alpha)
                    .setDepth(this.gameConfig.hud.depth);

                this.centerMessage[1]
                    .setAlpha(1)
                    .setDepth(this.gameConfig.background.effectsDepth);


            }

            this.centerMessage.forEach(t => {
                t
                    .setTint(color)
                    .setText(message);

                t.x = (this.scene.game.config.width / 2) - (t.width / 2);
                t.y = (this.scene.game.config.height / 2) - (t.height);
            });

            setTimeout(() => this.centerMessage.forEach(t => {
                    if (!showIfFunction || showIfFunction()) {
                        t
                            .setTint(color)
                            .setActive(true)
                            .setVisible(true);

                    }
                }
            ), 700);
        }

        hideCenterMessage() {
            if (this.centerMessage) {
                this.centerMessage.forEach(t =>
                    t
                        .setActive(false)
                        .setVisible(false)
                );
            }
        }
    }

    let instance = null;

    return {
        'get': (scene, gameConfig) => {
            if (!instance) {
                instance = new HudClass(scene, gameConfig);
            }
            return instance;
        }
    }

})();