/*eslint-disable block-scoped-var, id-length, no-control-regex, no-magic-numbers, no-prototype-builtins, no-redeclare, no-shadow, no-var, sort-vars*/
import * as $protobuf from "protobufjs/minimal";

// Common aliases
const $Reader = $protobuf.Reader, $Writer = $protobuf.Writer, $util = $protobuf.util;

// Exported root namespace
const $root = $protobuf.roots["default"] || ($protobuf.roots["default"] = {});

export const com = $root.com = (() => {

    /**
     * Namespace com.
     * @exports com
     * @namespace
     */
    const com = {};

    com.aetheris = (function() {

        /**
         * Namespace aetheris.
         * @memberof com
         * @namespace
         */
        const aetheris = {};

        aetheris.shared = (function() {

            /**
             * Namespace shared.
             * @memberof com.aetheris
             * @namespace
             */
            const shared = {};

            shared.proto = (function() {

                /**
                 * Namespace proto.
                 * @memberof com.aetheris.shared
                 * @namespace
                 */
                const proto = {};

                proto.GeoEntity = (function() {

                    /**
                     * Properties of a GeoEntity.
                     * @memberof com.aetheris.shared.proto
                     * @interface IGeoEntity
                     * @property {string|null} [id] GeoEntity id
                     * @property {com.aetheris.shared.proto.GeoEntity.EntityType|null} [type] GeoEntity type
                     * @property {number|null} [latitude] GeoEntity latitude
                     * @property {number|null} [longitude] GeoEntity longitude
                     * @property {number|null} [altitude] GeoEntity altitude
                     * @property {number|null} [velocity] GeoEntity velocity
                     * @property {number|null} [heading] GeoEntity heading
                     * @property {number|Long|null} [timestamp] GeoEntity timestamp
                     * @property {Uint8Array|null} [compressedMetadata] GeoEntity compressedMetadata
                     */

                    /**
                     * Constructs a new GeoEntity.
                     * @memberof com.aetheris.shared.proto
                     * @classdesc Represents a GeoEntity.
                     * @implements IGeoEntity
                     * @constructor
                     * @param {com.aetheris.shared.proto.IGeoEntity=} [properties] Properties to set
                     */
                    function GeoEntity(properties) {
                        if (properties)
                            for (let keys = Object.keys(properties), i = 0; i < keys.length; ++i)
                                if (properties[keys[i]] != null)
                                    this[keys[i]] = properties[keys[i]];
                    }

                    /**
                     * GeoEntity id.
                     * @member {string} id
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.id = "";

                    /**
                     * GeoEntity type.
                     * @member {com.aetheris.shared.proto.GeoEntity.EntityType} type
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.type = 0;

                    /**
                     * GeoEntity latitude.
                     * @member {number} latitude
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.latitude = 0;

                    /**
                     * GeoEntity longitude.
                     * @member {number} longitude
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.longitude = 0;

                    /**
                     * GeoEntity altitude.
                     * @member {number} altitude
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.altitude = 0;

                    /**
                     * GeoEntity velocity.
                     * @member {number} velocity
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.velocity = 0;

                    /**
                     * GeoEntity heading.
                     * @member {number} heading
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.heading = 0;

                    /**
                     * GeoEntity timestamp.
                     * @member {number|Long} timestamp
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.timestamp = $util.Long ? $util.Long.fromBits(0,0,false) : 0;

                    /**
                     * GeoEntity compressedMetadata.
                     * @member {Uint8Array} compressedMetadata
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     */
                    GeoEntity.prototype.compressedMetadata = $util.newBuffer([]);

                    /**
                     * Creates a new GeoEntity instance using the specified properties.
                     * @function create
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {com.aetheris.shared.proto.IGeoEntity=} [properties] Properties to set
                     * @returns {com.aetheris.shared.proto.GeoEntity} GeoEntity instance
                     */
                    GeoEntity.create = function create(properties) {
                        return new GeoEntity(properties);
                    };

                    /**
                     * Encodes the specified GeoEntity message. Does not implicitly {@link com.aetheris.shared.proto.GeoEntity.verify|verify} messages.
                     * @function encode
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {com.aetheris.shared.proto.IGeoEntity} message GeoEntity message or plain object to encode
                     * @param {$protobuf.Writer} [writer] Writer to encode to
                     * @returns {$protobuf.Writer} Writer
                     */
                    GeoEntity.encode = function encode(message, writer) {
                        if (!writer)
                            writer = $Writer.create();
                        if (message.id != null && Object.hasOwnProperty.call(message, "id"))
                            writer.uint32(/* id 1, wireType 2 =*/10).string(message.id);
                        if (message.type != null && Object.hasOwnProperty.call(message, "type"))
                            writer.uint32(/* id 2, wireType 0 =*/16).int32(message.type);
                        if (message.latitude != null && Object.hasOwnProperty.call(message, "latitude"))
                            writer.uint32(/* id 3, wireType 1 =*/25).double(message.latitude);
                        if (message.longitude != null && Object.hasOwnProperty.call(message, "longitude"))
                            writer.uint32(/* id 4, wireType 1 =*/33).double(message.longitude);
                        if (message.altitude != null && Object.hasOwnProperty.call(message, "altitude"))
                            writer.uint32(/* id 5, wireType 5 =*/45).float(message.altitude);
                        if (message.velocity != null && Object.hasOwnProperty.call(message, "velocity"))
                            writer.uint32(/* id 6, wireType 5 =*/53).float(message.velocity);
                        if (message.heading != null && Object.hasOwnProperty.call(message, "heading"))
                            writer.uint32(/* id 7, wireType 5 =*/61).float(message.heading);
                        if (message.timestamp != null && Object.hasOwnProperty.call(message, "timestamp"))
                            writer.uint32(/* id 8, wireType 0 =*/64).int64(message.timestamp);
                        if (message.compressedMetadata != null && Object.hasOwnProperty.call(message, "compressedMetadata"))
                            writer.uint32(/* id 9, wireType 2 =*/74).bytes(message.compressedMetadata);
                        return writer;
                    };

                    /**
                     * Encodes the specified GeoEntity message, length delimited. Does not implicitly {@link com.aetheris.shared.proto.GeoEntity.verify|verify} messages.
                     * @function encodeDelimited
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {com.aetheris.shared.proto.IGeoEntity} message GeoEntity message or plain object to encode
                     * @param {$protobuf.Writer} [writer] Writer to encode to
                     * @returns {$protobuf.Writer} Writer
                     */
                    GeoEntity.encodeDelimited = function encodeDelimited(message, writer) {
                        return this.encode(message, writer).ldelim();
                    };

                    /**
                     * Decodes a GeoEntity message from the specified reader or buffer.
                     * @function decode
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                     * @param {number} [length] Message length if known beforehand
                     * @returns {com.aetheris.shared.proto.GeoEntity} GeoEntity
                     * @throws {Error} If the payload is not a reader or valid buffer
                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                     */
                    GeoEntity.decode = function decode(reader, length, error) {
                        if (!(reader instanceof $Reader))
                            reader = $Reader.create(reader);
                        let end = length === undefined ? reader.len : reader.pos + length, message = new $root.com.aetheris.shared.proto.GeoEntity();
                        while (reader.pos < end) {
                            let tag = reader.uint32();
                            if (tag === error)
                                break;
                            switch (tag >>> 3) {
                            case 1: {
                                    message.id = reader.string();
                                    break;
                                }
                            case 2: {
                                    message.type = reader.int32();
                                    break;
                                }
                            case 3: {
                                    message.latitude = reader.double();
                                    break;
                                }
                            case 4: {
                                    message.longitude = reader.double();
                                    break;
                                }
                            case 5: {
                                    message.altitude = reader.float();
                                    break;
                                }
                            case 6: {
                                    message.velocity = reader.float();
                                    break;
                                }
                            case 7: {
                                    message.heading = reader.float();
                                    break;
                                }
                            case 8: {
                                    message.timestamp = reader.int64();
                                    break;
                                }
                            case 9: {
                                    message.compressedMetadata = reader.bytes();
                                    break;
                                }
                            default:
                                reader.skipType(tag & 7);
                                break;
                            }
                        }
                        return message;
                    };

                    /**
                     * Decodes a GeoEntity message from the specified reader or buffer, length delimited.
                     * @function decodeDelimited
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {$protobuf.Reader|Uint8Array} reader Reader or buffer to decode from
                     * @returns {com.aetheris.shared.proto.GeoEntity} GeoEntity
                     * @throws {Error} If the payload is not a reader or valid buffer
                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                     */
                    GeoEntity.decodeDelimited = function decodeDelimited(reader) {
                        if (!(reader instanceof $Reader))
                            reader = new $Reader(reader);
                        return this.decode(reader, reader.uint32());
                    };

                    /**
                     * Verifies a GeoEntity message.
                     * @function verify
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {Object.<string,*>} message Plain object to verify
                     * @returns {string|null} `null` if valid, otherwise the reason why it is not
                     */
                    GeoEntity.verify = function verify(message) {
                        if (typeof message !== "object" || message === null)
                            return "object expected";
                        if (message.id != null && message.hasOwnProperty("id"))
                            if (!$util.isString(message.id))
                                return "id: string expected";
                        if (message.type != null && message.hasOwnProperty("type"))
                            switch (message.type) {
                            default:
                                return "type: enum value expected";
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                break;
                            }
                        if (message.latitude != null && message.hasOwnProperty("latitude"))
                            if (typeof message.latitude !== "number")
                                return "latitude: number expected";
                        if (message.longitude != null && message.hasOwnProperty("longitude"))
                            if (typeof message.longitude !== "number")
                                return "longitude: number expected";
                        if (message.altitude != null && message.hasOwnProperty("altitude"))
                            if (typeof message.altitude !== "number")
                                return "altitude: number expected";
                        if (message.velocity != null && message.hasOwnProperty("velocity"))
                            if (typeof message.velocity !== "number")
                                return "velocity: number expected";
                        if (message.heading != null && message.hasOwnProperty("heading"))
                            if (typeof message.heading !== "number")
                                return "heading: number expected";
                        if (message.timestamp != null && message.hasOwnProperty("timestamp"))
                            if (!$util.isInteger(message.timestamp) && !(message.timestamp && $util.isInteger(message.timestamp.low) && $util.isInteger(message.timestamp.high)))
                                return "timestamp: integer|Long expected";
                        if (message.compressedMetadata != null && message.hasOwnProperty("compressedMetadata"))
                            if (!(message.compressedMetadata && typeof message.compressedMetadata.length === "number" || $util.isString(message.compressedMetadata)))
                                return "compressedMetadata: buffer expected";
                        return null;
                    };

                    /**
                     * Creates a GeoEntity message from a plain object. Also converts values to their respective internal types.
                     * @function fromObject
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {Object.<string,*>} object Plain object
                     * @returns {com.aetheris.shared.proto.GeoEntity} GeoEntity
                     */
                    GeoEntity.fromObject = function fromObject(object) {
                        if (object instanceof $root.com.aetheris.shared.proto.GeoEntity)
                            return object;
                        let message = new $root.com.aetheris.shared.proto.GeoEntity();
                        if (object.id != null)
                            message.id = String(object.id);
                        switch (object.type) {
                        default:
                            if (typeof object.type === "number") {
                                message.type = object.type;
                                break;
                            }
                            break;
                        case "UNKNOWN":
                        case 0:
                            message.type = 0;
                            break;
                        case "FLIGHT":
                        case 1:
                            message.type = 1;
                            break;
                        case "SHIP":
                        case 2:
                            message.type = 2;
                            break;
                        case "SATELLITE":
                        case 3:
                            message.type = 3;
                            break;
                        }
                        if (object.latitude != null)
                            message.latitude = Number(object.latitude);
                        if (object.longitude != null)
                            message.longitude = Number(object.longitude);
                        if (object.altitude != null)
                            message.altitude = Number(object.altitude);
                        if (object.velocity != null)
                            message.velocity = Number(object.velocity);
                        if (object.heading != null)
                            message.heading = Number(object.heading);
                        if (object.timestamp != null)
                            if ($util.Long)
                                (message.timestamp = $util.Long.fromValue(object.timestamp)).unsigned = false;
                            else if (typeof object.timestamp === "string")
                                message.timestamp = parseInt(object.timestamp, 10);
                            else if (typeof object.timestamp === "number")
                                message.timestamp = object.timestamp;
                            else if (typeof object.timestamp === "object")
                                message.timestamp = new $util.LongBits(object.timestamp.low >>> 0, object.timestamp.high >>> 0).toNumber();
                        if (object.compressedMetadata != null)
                            if (typeof object.compressedMetadata === "string")
                                $util.base64.decode(object.compressedMetadata, message.compressedMetadata = $util.newBuffer($util.base64.length(object.compressedMetadata)), 0);
                            else if (object.compressedMetadata.length >= 0)
                                message.compressedMetadata = object.compressedMetadata;
                        return message;
                    };

                    /**
                     * Creates a plain object from a GeoEntity message. Also converts values to other types if specified.
                     * @function toObject
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {com.aetheris.shared.proto.GeoEntity} message GeoEntity
                     * @param {$protobuf.IConversionOptions} [options] Conversion options
                     * @returns {Object.<string,*>} Plain object
                     */
                    GeoEntity.toObject = function toObject(message, options) {
                        if (!options)
                            options = {};
                        let object = {};
                        if (options.defaults) {
                            object.id = "";
                            object.type = options.enums === String ? "UNKNOWN" : 0;
                            object.latitude = 0;
                            object.longitude = 0;
                            object.altitude = 0;
                            object.velocity = 0;
                            object.heading = 0;
                            if ($util.Long) {
                                let long = new $util.Long(0, 0, false);
                                object.timestamp = options.longs === String ? long.toString() : options.longs === Number ? long.toNumber() : long;
                            } else
                                object.timestamp = options.longs === String ? "0" : 0;
                            if (options.bytes === String)
                                object.compressedMetadata = "";
                            else {
                                object.compressedMetadata = [];
                                if (options.bytes !== Array)
                                    object.compressedMetadata = $util.newBuffer(object.compressedMetadata);
                            }
                        }
                        if (message.id != null && message.hasOwnProperty("id"))
                            object.id = message.id;
                        if (message.type != null && message.hasOwnProperty("type"))
                            object.type = options.enums === String ? $root.com.aetheris.shared.proto.GeoEntity.EntityType[message.type] === undefined ? message.type : $root.com.aetheris.shared.proto.GeoEntity.EntityType[message.type] : message.type;
                        if (message.latitude != null && message.hasOwnProperty("latitude"))
                            object.latitude = options.json && !isFinite(message.latitude) ? String(message.latitude) : message.latitude;
                        if (message.longitude != null && message.hasOwnProperty("longitude"))
                            object.longitude = options.json && !isFinite(message.longitude) ? String(message.longitude) : message.longitude;
                        if (message.altitude != null && message.hasOwnProperty("altitude"))
                            object.altitude = options.json && !isFinite(message.altitude) ? String(message.altitude) : message.altitude;
                        if (message.velocity != null && message.hasOwnProperty("velocity"))
                            object.velocity = options.json && !isFinite(message.velocity) ? String(message.velocity) : message.velocity;
                        if (message.heading != null && message.hasOwnProperty("heading"))
                            object.heading = options.json && !isFinite(message.heading) ? String(message.heading) : message.heading;
                        if (message.timestamp != null && message.hasOwnProperty("timestamp"))
                            if (typeof message.timestamp === "number")
                                object.timestamp = options.longs === String ? String(message.timestamp) : message.timestamp;
                            else
                                object.timestamp = options.longs === String ? $util.Long.prototype.toString.call(message.timestamp) : options.longs === Number ? new $util.LongBits(message.timestamp.low >>> 0, message.timestamp.high >>> 0).toNumber() : message.timestamp;
                        if (message.compressedMetadata != null && message.hasOwnProperty("compressedMetadata"))
                            object.compressedMetadata = options.bytes === String ? $util.base64.encode(message.compressedMetadata, 0, message.compressedMetadata.length) : options.bytes === Array ? Array.prototype.slice.call(message.compressedMetadata) : message.compressedMetadata;
                        return object;
                    };

                    /**
                     * Converts this GeoEntity to JSON.
                     * @function toJSON
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @instance
                     * @returns {Object.<string,*>} JSON object
                     */
                    GeoEntity.prototype.toJSON = function toJSON() {
                        return this.constructor.toObject(this, $protobuf.util.toJSONOptions);
                    };

                    /**
                     * Gets the default type url for GeoEntity
                     * @function getTypeUrl
                     * @memberof com.aetheris.shared.proto.GeoEntity
                     * @static
                     * @param {string} [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                     * @returns {string} The default type url
                     */
                    GeoEntity.getTypeUrl = function getTypeUrl(typeUrlPrefix) {
                        if (typeUrlPrefix === undefined) {
                            typeUrlPrefix = "type.googleapis.com";
                        }
                        return typeUrlPrefix + "/com.aetheris.shared.proto.GeoEntity";
                    };

                    /**
                     * EntityType enum.
                     * @name com.aetheris.shared.proto.GeoEntity.EntityType
                     * @enum {number}
                     * @property {number} UNKNOWN=0 UNKNOWN value
                     * @property {number} FLIGHT=1 FLIGHT value
                     * @property {number} SHIP=2 SHIP value
                     * @property {number} SATELLITE=3 SATELLITE value
                     */
                    GeoEntity.EntityType = (function() {
                        const valuesById = {}, values = Object.create(valuesById);
                        values[valuesById[0] = "UNKNOWN"] = 0;
                        values[valuesById[1] = "FLIGHT"] = 1;
                        values[valuesById[2] = "SHIP"] = 2;
                        values[valuesById[3] = "SATELLITE"] = 3;
                        return values;
                    })();

                    return GeoEntity;
                })();

                return proto;
            })();

            return shared;
        })();

        return aetheris;
    })();

    return com;
})();

export { $root as default };
