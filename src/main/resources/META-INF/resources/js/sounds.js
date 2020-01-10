const Sounds = (function () {

    const LP = [
        '',
        'lp1000',
        'lp2000',
        'lp4000',
        'lp6000',
    ];

    class SoundClass {
        constructor(scene, gameConfig, file) {
            this.file = file;
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.sounds = {};
            this.pool = [];
            this.priorityIndex = 0;
            this.debounce = 0;
            this.lastTime = 0;
            this.distanceThreshold = 0;
        }

        preload() {
            LP.forEach(v => {
                this.scene.load.audio(`${this.file}-${v}-l`, `assets/sound/${this.file}/${this.file}-${v}-l.mp3`);
                this.scene.load.audio(`${this.file}-${v}-r`, `assets/sound/${this.file}/${this.file}-${v}-r.mp3`);
            });
        }

        _create() {
            const sounds = {};

            LP.forEach(v => {
                sounds[v] = [
                    this.scene.sound.add(`${this.file}-${v}-l`),
                    this.scene.sound.add(`${this.file}-${v}-r`)
                ];
            });
            return sounds;
        }

        create(poolSize, debounce, distanceThreshold) {
            this.debounce = debounce;
            this.distanceThreshold = distanceThreshold;
            for (let i = 0; i < poolSize; i++) {
                this.pool.push(this._create());
            }

            return this;
        }

        getNextSounds(f, priority) {
            if (priority) {
                this.priorityIndex = (this.priorityIndex + 1) % this.pool.length;
                const s = this.pool[this.priorityIndex][f];
                if (s.isPlaying) {
                    s.stop();
                }
                return s;
            }
            const s = this.pool.find(s => !s[f][0].isPlaying);
            return s ? s[f] : null;
        }

        play3d(sourceX, sourceY, priority) {
            let dist = Math.pow((this.gameConfig.world.max.x -
                Math.sqrt(
                    Math.pow(this.scene.cameras.main.worldView.centerX - sourceX, 2) +
                    Math.pow(this.scene.cameras.main.worldView.centerY - sourceY, 2)
                )) / this.gameConfig.world.max.x, 8);

            if (dist > this.distanceThreshold || priority) {
                let f = LP[0];
                if (dist < 0.8) {
                    f = LP[1];
                } else if (dist < 0.85) {
                    f = LP[2];
                } else if (dist < 0.9) {
                    f = LP[3];
                } else if (dist < 0.95) {
                    f = LP[4];
                }
                let pan = (sourceX - this.scene.cameras.main.worldView.centerX) / 600;
                pan = Math.max(-1, Math.min(1, pan));

                if (!priority) {
                    dist = dist * 0.9;
                }

                this.play(dist, pan, f, priority);
            }


        }

        /**
         *
         * @param vol 0-1
         * @param pan -1-+1
         * @param filter lp1000, lp2000, lp4000, lp6000, lp8000
         * @param priority use priority pool
         */
        play(vol, pan, filter, priority) {
            if (!priority &&
                this.debounce &&
                Date.now() - this.lastTime < this.debounce) {
                return;
            }

            if (!filter) {
                filter = '';
            }

            const sounds = this.getNextSounds(filter, priority);
            if (sounds) {
                this.lastTime = Date.now();

                if (!pan) {
                    pan = 0;
                }

                sounds[0].play({
                    'volume': vol * (pan > 0 ? 1 - pan : 1)
                });

                sounds[1].play({
                    'volume': vol * (pan < 0 ? 1 - -pan : 1)
                });

            }

        }
    }

    class SoundsClass {
        constructor(scene, gameConfig) {
            this.scene = scene;
            this.gameConfig = gameConfig;
            this.sounds = {};
        }

        get(s) {
            if (!this.sounds[s]) {
                this.sounds[s] = new SoundClass(this.scene, this.gameConfig, s);
            }
            return this.sounds[s];
        }
    }

    let instance;

    return {
        'get': (scene, gameConfig) => {
            if (!instance) {
                instance = new SoundsClass(scene, gameConfig);
            }
            return instance;
        }
    }
})();