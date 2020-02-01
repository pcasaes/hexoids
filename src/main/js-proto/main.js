const Pbf = require('pbf');
const hexoidsProto = require('./hexoids.proto.node.js');


const ProtoProcessor = (function () {

    class ProtoProcessorClass {
        constructor() {
        }

        readDto(buffer) {
            const pbf = new Pbf(buffer);
            return hexoidsProto.Dto.read(pbf);
        }

        writeRequestCommand(command) {
            const pbf = new Pbf();
            hexoidsProto.RequestCommand.write(command, pbf);
            return pbf.finish();
        }
    }

    return new ProtoProcessorClass();
})();

try {
    window.ProtoProcessor = ProtoProcessor;
} catch (ex) {
    console.log("no window object");
}

try {
    module.exports = ProtoProcessor;
} catch (ex) {
    console.log("no modle exports");
}
