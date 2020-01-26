const Pbf = require('pbf');
const bbopProto = require('./bbop.proto.node.js');


const ProtoProcessor = (function () {

    class ProtoProcessorClass {
        constructor() {
        }

        readDto(buffer) {
            const pbf = new Pbf(buffer);
            return bbopProto.Dto.read(pbf);
        }

        writeRequestCommand(command) {
            const pbf = new Pbf();
            bbopProto.RequestCommand.write(command, pbf);
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
