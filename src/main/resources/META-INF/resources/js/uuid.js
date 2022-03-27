// https://github.com/uuidjs/uuid/blob/16e9cc9017663a24588c4925bb3e63ae624ad1d4/src/parse.js
function uuidParse(uuidStr) {
    let v;
    const arr = new Uint8Array(16);

    // Parse ########-....-....-....-............
    arr[0] = (v = parseInt(uuidStr.slice(0, 8), 16)) >>> 24;
    arr[1] = (v >>> 16) & 0xff;
    arr[2] = (v >>> 8) & 0xff;
    arr[3] = v & 0xff;

    // Parse ........-####-....-....-............
    arr[4] = (v = parseInt(uuidStr.slice(9, 13), 16)) >>> 8;
    arr[5] = v & 0xff;

    // Parse ........-....-####-....-............
    arr[6] = (v = parseInt(uuidStr.slice(14, 18), 16)) >>> 8;
    arr[7] = v & 0xff;

    // Parse ........-....-....-####-............
    arr[8] = (v = parseInt(uuidStr.slice(19, 23), 16)) >>> 8;
    arr[9] = v & 0xff;

    // Parse ........-....-....-....-############
    // (Use "/" to avoid 32-bit truncation when bit-shifting high-order bytes)
    arr[10] = ((v = parseInt(uuidStr.slice(24, 36), 16)) / 0x10000000000) & 0xff;
    arr[11] = (v / 0x100000000) & 0xff;
    arr[12] = (v >>> 24) & 0xff;
    arr[13] = (v >>> 16) & 0xff;
    arr[14] = (v >>> 8) & 0xff;
    arr[15] = v & 0xff;

    return arr;

}

function uuid(s) {
    const idStr = !s ? crypto.randomUUID() : s;

    const idBytes = uuidParse(idStr);

    return {
        str: idStr,
        id: Array.from(idBytes),
        bytes: idBytes
    };
}


try {
    module.exports = uuid;
} catch (ex) {
    console.debug("Could not export module. Only needed in nodejs. " + ex);
}