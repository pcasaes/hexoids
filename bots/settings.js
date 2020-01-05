let local;

try {
    local = require('./settings.local');
} catch (err) {
    console.warn("No local config found: " + err);
    local = {};
}

const fixed = {
    "host": "localhost:8080",
    "bots": 3,
};

module.exports = {...fixed, ...local};