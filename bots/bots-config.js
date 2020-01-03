let local;

try {
    local = require('./bots-config-local');
} catch (err) {
    console.warn("No local config found: " + err);
    local = {};
}

const fixed = {
    "host": "localhost:8080"
};

module.exports = {...fixed, ...local};