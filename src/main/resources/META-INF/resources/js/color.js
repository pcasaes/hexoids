const Colors = (function () {

    /**
     * Source: https://gist.github.com/mjackson/5311256
     */

    /**
     * Converts an RGB color value to HSL. Conversion formula
     * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
     * Assumes r, g, and b are contained in the set [0, 255] and
     * returns h, s, and l in the set [0, 1].
     *
     * @param   Number  r       The red color value
     * @param   Number  g       The green color value
     * @param   Number  b       The blue color value
     * @return  Array           The HSL representation
     */
    function rgbToHsl(r, g, b) {
        r /= 255;
        g /= 255;
        b /= 255;

        const max = Math.max(r, g, b);
        const min = Math.min(r, g, b);
        let h, s, l = (max + min) / 2;

        if (max === min) {
            h = s = 0; // achromatic
        } else {
            let d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

            switch (max) {
                case r:
                    h = (g - b) / d + (g < b ? 6 : 0);
                    break;
                case g:
                    h = (b - r) / d + 2;
                    break;
                case b:
                    h = (r - g) / d + 4;
                    break;
            }

            h /= 6;
        }

        return [h, s, l];
    }

    function hue2rgb(p, q, t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1 / 6) return p + (q - p) * 6 * t;
        if (t < 1 / 2) return q;
        if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
        return p;
    }

    /**
     * Converts an HSL color value to RGB. Conversion formula
     * adapted from http://en.wikipedia.org/wiki/HSL_color_space.
     * Assumes h, s, and l are contained in the set [0, 1] and
     * returns r, g, and b in the set [0, 255].
     *
     * @param   Number  h       The hue
     * @param   Number  s       The saturation
     * @param   Number  l       The lightness
     * @return  Array           The RGB representation
     */
    function hslToRgb(h, s, l) {
        let r, g, b;

        if (s === 0) {
            r = g = b = l; // achromatic
        } else {
            const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            const p = 2 * l - q;

            r = hue2rgb(p, q, h + 1 / 3);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1 / 3);
        }

        return [r * 255, g * 255, b * 255];
    }

    /**
     * Converts an RGB color value to HSV. Conversion formula
     * adapted from http://en.wikipedia.org/wiki/HSV_color_space.
     * Assumes r, g, and b are contained in the set [0, 255] and
     * returns h, s, and v in the set [0, 1].
     *
     * @param   Number  r       The red color value
     * @param   Number  g       The green color value
     * @param   Number  b       The blue color value
     * @return  Array           The HSV representation
     */
    function rgbToHsv(r, g, b) {
        r /= 255;
        g /= 255;
        b /= 255;

        const max = Math.max(r, g, b);
        const min = Math.min(r, g, b);
        let h, s, v = max;

        var d = max - min;
        s = max === 0 ? 0 : d / max;

        if (max === min) {
            h = 0; // achromatic
        } else {
            switch (max) {
                case r:
                    h = (g - b) / d + (g < b ? 6 : 0);
                    break;
                case g:
                    h = (b - r) / d + 2;
                    break;
                case b:
                    h = (r - g) / d + 4;
                    break;
            }

            h /= 6;
        }

        return [h, s, v];
    }

    /**
     * Converts an HSV color value to RGB. Conversion formula
     * adapted from http://en.wikipedia.org/wiki/HSV_color_space.
     * Assumes h, s, and v are contained in the set [0, 1] and
     * returns r, g, and b in the set [0, 255].
     *
     * @param   Number  h       The hue
     * @param   Number  s       The saturation
     * @param   Number  v       The value
     * @return  Array           The RGB representation
     */
    function hsvToRgb(h, s, v) {
        let r, g, b;

        let i = Math.floor(h * 6);
        let f = h * 6 - i;
        let p = v * (1 - s);
        let q = v * (1 - f * s);
        let t = v * (1 - (1 - f) * s);

        switch (i % 6) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            case 5:
                r = v;
                g = p;
                b = q;
                break;
        }

        return [r * 255, g * 255, b * 255];
    }

    /**
     * END https://gist.github.com/mjackson/5311256
     */


    class ColorClass {
        constructor() {
            this.hsl = {};
            this.rgb = {};
            this.rgbNumber = null;
            this.hslNumber = null;
            this.rgbColorString = null;
        }

        toRgbNumber() {
            if (!this.rgbNumber) {
                this.rgbNumber = this.rgb.r * 0x10000 + this.rgb.g * 0x100 + this.rgb.b;
            }
            return this.rgbNumber;
        }

        toHslNumber() {
            if (!this.hslNumber) {
                this.hslNumber = this.hsl.h * 0x10000 + this.hsl.s * 0x100 + this.hsl.l;
            }
            return this.hslNumber;
        }

        toRgbColorString() {
            if (!this.rgbColorString) {
                let r = Number(this.toRgbNumber()).toString(16);
                while (r.length < 6) {
                    r = "0" + r;
                }
                return "#" + r;
            }
            return this.rgbColorString;
        }

        transformByDegrees(degrees) {
            if (degrees < 0) {
                degrees = 360 + degrees;
            }

            if (degrees > 360) {
                degrees = degrees % 360;
            }

            const h = ((this.hsl.h * 360 + degrees) % 360) / 360;

            return fromHsl(h, this.hsl.s, this.hsl.l);
        }

        scaleLight(scale) {
            const l = this.hsl.l * scale;
            return fromHsl(this.hsl.h, this.hsl.s, l);
        }
    }

    function getComponents(number) {
        const str = Number(number).toString(16);

        const r = [];
        let j = 0;
        for (let i = 0; i < 6; i += 2) {
            const inv = 6 - (i + 1);
            if (str.length < inv) {
                r.push(0);
            } else {
                const len = (Math.min(str.length, 6 - i) - inv) + 1;
                r.push(Number("0x" + str.substr(j, len)));
                j += 2;
            }
        }


        return r;
    }

    function fromRgbNumber(number) {
        const p = getComponents(number);

        const c = new ColorClass();
        c.rgb.r = p[0];
        c.rgb.g = p[1];
        c.rgb.b = p[2];

        const hsl = rgbToHsl(c.rgb.r, c.rgb.g, c.rgb.b);
        c.hsl.h = hsl[0];
        c.hsl.s = hsl[1];
        c.hsl.l = hsl[2];

        return c;
    }

    function fromHsl(h, s, l) {
        const c = new ColorClass();
        c.hsl.h = h;
        c.hsl.s = s;
        c.hsl.l = l;

        const rgb = hslToRgb(c.hsl.h, c.hsl.s, c.hsl.l);
        c.rgb.r = Math.round(rgb[0]);
        c.rgb.g = Math.round(rgb[1]);
        c.rgb.b = Math.round(rgb[2]);

        return c;
    }

    function fromHslNumber(number) {
        const p = getComponents(number);
        return fromHsl(p[0] / 0xff, p[1] / 0xff, p[2] / 0xff);
    }

    class ColorsClass {
        constructor() {
            this.fromHsl = fromHsl;
            this.fromRgbNumber = fromRgbNumber;
            this.fromHslNumber = fromHslNumber;

            this.palette = null;
            this.primaryColor = null;
            this._size = null;
            this.darkTextColor = null;
        }

        create(primaryColor, hues, offset) {
            this._size = hues * 2;
            this.palette = [];
            this.primaryColor = primaryColor;
            const deg = 360 / hues;
            this.palette.push(this.fromRgbNumber(this.getPrimaryColor()));
            this.palette.push(this.palette[0].transformByDegrees(offset));
            for (let i = 0; i < this.size(); i += 2) {
                const c = this.palette[i].transformByDegrees(deg);
                this.palette.push(c);
                this.palette.push(c.transformByDegrees(offset));
            }

            this.darkTextColor = this.fromRgbNumber(0x777777);

            return this;
        }

        size() {
            return this._size;
        }

        get(i) {
            return this.palette[i % this.size()];
        }

        getDarkTextColor() {
            return this.darkTextColor;
        }

        getPrimaryColor() {
            /*
                    0xaa0022, //redish
                    0xaa3300, //orangish
                    0x00aa88, //greenish
                    0x00aa33, //greenish 2
                    0x0077ff, //blueish
                    0x00d0ff, //light bluish
                    0xffaa00, //yellowish
                    0x00a0bb, //cyanish
                    0xbb00a0, //pinkish
                    0x8700ff, //purplish
            */
            return this.primaryColor; //0x00B46B;
        }
    }

    return new ColorsClass().create(0xff8800, 6, 20);
})();

try {
    module.exports = Colors;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}