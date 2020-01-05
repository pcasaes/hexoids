const Optional = (function () {

    class OptionalClass {
        constructor(value) {
            this.value = value;
        }

        ifPresent(f, otherwise) {
            if (this.value) {
                return f(this.value);
            } else if (typeof otherwise === 'function') {
                return otherwise();
            }
        }

        filter(f) {
            if (this.value && f(this.value)) {
                return this;
            }
            return empty;
        }

        map(f) {
            if (this.value) {
                return new OptionalClass(f(this.value));
            }
            return empty;
        }

        orElse(v) {
            return this.value || v;
        }

    }

    const empty = new OptionalClass(null);


    return {
        'of': (v) => {
            if (!v) {
                return empty;
            }
            return new OptionalClass(v);
        },
        'empty': () => empty,
    }
})();

try {
    module.exports = Optional;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}